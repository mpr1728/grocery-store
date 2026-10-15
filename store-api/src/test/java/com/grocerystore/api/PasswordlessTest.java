package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.account.AccountDtos.ConfirmSignUpRequest;
import com.grocerystore.api.account.AccountDtos.EmailCodeSent;
import com.grocerystore.api.account.AccountDtos.EmailVerified;
import com.grocerystore.api.account.AccountDtos.FinishEmailSignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountDtos.VerifyEmailCodeRequest;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.account.Mailer;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyOptions;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyResponse;
import com.grocerystore.api.passkey.PasskeyService;

/** Sign-up and sign-in with a code sent to the email address, and the parts of passkeys a test can reach. */
@SpringBootTest
@ActiveProfiles("test")
class PasswordlessTest {

    static class FakeMailer implements Mailer {
        String lastTo;
        String lastText;

        @Override
        public void send(String to, String subject, String text) {
            lastTo = to;
            lastText = text;
        }

        String lastCode() {
            Matcher code = Pattern.compile("\\b[0-9]{6}\\b").matcher(lastText);
            assertThat(code.find()).isTrue();
            return code.group();
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        FakeMailer fakeMailer() {
            return new FakeMailer();
        }
    }

    private static int sequence = 0;

    @Autowired
    AccountService accounts;
    @Autowired
    PasskeyService passkeys;
    @Autowired
    FakeMailer mailer;

    private static String newEmail() {
        return "shopper" + System.nanoTime() + "." + (sequence++) + "@yahoo.com";
    }

    @Test
    void aNewAddressGetsAnAccountAfterTheCodeAndAName() {
        String email = newEmail();
        EmailCodeSent sent = accounts.sendEmailCode("  " + email.toUpperCase() + " ");
        assertThat(sent.sentTo()).isEqualTo(email);
        assertThat(mailer.lastTo).isEqualTo(email);

        EmailVerified verified = accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), mailer.lastCode()));
        assertThat(verified.needsProfile()).isTrue();
        assertThat(verified.token()).isNull();

        SignedIn signedIn = accounts.finishEmailSignUp(new FinishEmailSignUpRequest(sent.codeId(), "Meera", null));
        assertThat(signedIn.account().email()).isEqualTo(email);
        assertThat(signedIn.account().phone()).isNull();
        assertThat(signedIn.account().pointsBalance()).isEqualTo(50);
        assertThat(accounts.require("Bearer " + signedIn.token()).hasAccount()).isTrue();
        // The code is used up.
        assertThatThrownBy(() -> accounts.finishEmailSignUp(new FinishEmailSignUpRequest(sent.codeId(), "Again", null)))
                .isInstanceOf(BadRequestException.class);
        // A passwordless account cannot be entered with a password.
        assertThatThrownBy(() -> accounts.signIn(new SignInRequest(email, "anything")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aKnownAddressIsSignedInStraightAway() {
        String email = newEmail();
        EmailCodeSent first = accounts.sendEmailCode(email);
        accounts.verifyEmailCode(new VerifyEmailCodeRequest(first.codeId(), mailer.lastCode()));
        accounts.finishEmailSignUp(new FinishEmailSignUpRequest(first.codeId(), "Meera", "9123456780"));

        EmailCodeSent again = accounts.sendEmailCode(email);
        EmailVerified verified = accounts.verifyEmailCode(new VerifyEmailCodeRequest(again.codeId(), mailer.lastCode()));
        assertThat(verified.needsProfile()).isFalse();
        assertThat(verified.account().name()).isEqualTo("Meera");
        assertThat(accounts.require("Bearer " + verified.token()).getEmail()).isEqualTo(email);
    }

    @Test
    void aPasswordAccountCanUseACodeInsteadOfAForgottenPassword() {
        String email = newEmail();
        String phone = String.valueOf(7000000000L + (System.nanoTime() % 999999999L));
        String signupId = accounts.signUp(new SignUpRequest("Old Timer", phone, email, "forgotten one")).signupId();
        accounts.confirmSignUp(new ConfirmSignUpRequest(signupId, mailer.lastCode()));

        EmailCodeSent sent = accounts.sendEmailCode(email);
        EmailVerified verified = accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), mailer.lastCode()));
        assertThat(verified.needsProfile()).isFalse();
        assertThat(verified.account().name()).isEqualTo("Old Timer");
    }

    @Test
    void wrongCodesRunOutAndSendingIsLimited() {
        String email = newEmail();
        EmailCodeSent sent = accounts.sendEmailCode(email);
        String right = mailer.lastCode();
        String wrong = right.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), wrong)))
                    .isInstanceOf(BadRequestException.class);
        }
        // After five wrong tries even the right code is refused.
        assertThatThrownBy(() -> accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), right)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("no longer valid");

        accounts.sendEmailCode(email);
        accounts.sendEmailCode(email);
        assertThatThrownBy(() -> accounts.sendEmailCode(email))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Too many codes");
    }

    @Test
    void passkeyOptionsAreSingleUseAndBoundToTheAccount() {
        String email = newEmail();
        EmailCodeSent sent = accounts.sendEmailCode(email);
        accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), mailer.lastCode()));
        SignedIn member = accounts.finishEmailSignUp(new FinishEmailSignUpRequest(sent.codeId(), "Keya", null));
        Long customerId = accounts.require("Bearer " + member.token()).getId();

        CeremonyOptions options = passkeys.startRegistration(customerId);
        assertThat(options.optionsJson()).contains("\"rp\"").contains("\"residentKey\":\"required\"").contains(email);

        // A made-up answer is refused, and the options cannot be tried a second time.
        CeremonyResponse fake = new CeremonyResponse(options.ceremonyId(),
                "{\"id\":\"AAAA\",\"rawId\":\"AAAA\",\"type\":\"public-key\",\"response\":{\"clientDataJSON\":\"e30\",\"attestationObject\":\"oA\"},\"clientExtensionResults\":{}}",
                "Test");
        assertThatThrownBy(() -> passkeys.finishRegistration(customerId, fake)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> passkeys.finishRegistration(customerId, fake))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("expired");

        CeremonyOptions signIn = passkeys.startSignIn();
        assertThat(signIn.optionsJson()).contains("\"challenge\"").contains("\"userVerification\":\"required\"");
        assertThat(passkeys.list(customerId)).isEmpty();
    }
}
