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
import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignUpResponse;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.account.Mailer;
import com.grocerystore.api.common.BadRequestException;

/** Sign-up where email can be sent: the account exists only after the emailed code is confirmed. */
@SpringBootTest
@ActiveProfiles("test")
class EmailSignUpTest {

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

    private static long phoneSequence = 8000000000L;

    @Autowired
    AccountService accounts;
    @Autowired
    FakeMailer mailer;

    @Test
    void theEmailedCodeCreatesTheAccount() {
        String phone = String.valueOf(phoneSequence++);
        SignUpResponse started = accounts.signUp(new SignUpRequest("Ravi", phone, "Ravi." + phone + "@Example.com", "correct horse"));

        assertThat(started.token()).isNull();
        assertThat(started.sentTo()).isEqualTo("ravi." + phone + "@example.com");
        assertThat(mailer.lastTo).isEqualTo(started.sentTo());
        // Nothing can sign in until the code is confirmed.
        assertThatThrownBy(() -> accounts.signIn(new SignInRequest(phone, "correct horse")))
                .isInstanceOf(BadRequestException.class);

        SignedIn signedIn = accounts.confirmSignUp(new ConfirmSignUpRequest(started.signupId(), mailer.lastCode()));

        assertThat(signedIn.account().email()).isEqualTo(started.sentTo());
        assertThat(signedIn.account().pointsBalance()).isEqualTo(50);
        assertThat(accounts.signIn(new SignInRequest(started.sentTo(), "correct horse")).account().phone())
                .isEqualTo("+91" + phone);
        // A code works once.
        assertThatThrownBy(() -> accounts.confirmSignUp(new ConfirmSignUpRequest(started.signupId(), mailer.lastCode())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void anAccountNeedsNoPhoneAndAcceptsAUsNumber() {
        long n = phoneSequence++;
        SignUpResponse emailOnly = accounts.signUp(new SignUpRequest("Meera", null, "meera" + n + "@example.com", "correct horse"));
        SignedIn meera = accounts.confirmSignUp(new ConfirmSignUpRequest(emailOnly.signupId(), mailer.lastCode()));
        assertThat(meera.account().phone()).isNull();

        String usNumber = "+1415555" + String.format("%04d", n % 10000);
        SignUpResponse fromUs = accounts.signUp(new SignUpRequest("Sam", usNumber, "sam" + n + "@example.com", "correct horse"));
        SignedIn sam = accounts.confirmSignUp(new ConfirmSignUpRequest(fromUs.signupId(), mailer.lastCode()));
        assertThat(sam.account().phone()).isEqualTo(usNumber);
        assertThat(accounts.signIn(new SignInRequest(usNumber, "correct horse")).account().email())
                .isEqualTo("sam" + n + "@example.com");
    }

    @Test
    void wrongCodesAreCountedAndThenTheSignUpIsLocked() {
        String phone = String.valueOf(phoneSequence++);
        SignUpResponse started = accounts.signUp(new SignUpRequest("Ravi", phone, phone + "@example.com", "correct horse"));
        String right = mailer.lastCode();
        String wrong = right.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> accounts.confirmSignUp(new ConfirmSignUpRequest(started.signupId(), wrong)))
                    .isInstanceOf(BadRequestException.class);
        }
        assertThatThrownBy(() -> accounts.confirmSignUp(new ConfirmSignUpRequest(started.signupId(), right)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void onlyAFewCodesGoToOneAddress() {
        String phone = String.valueOf(phoneSequence++);
        SignUpRequest request = new SignUpRequest("Ravi", phone, phone + "@example.com", "correct horse");
        for (int i = 0; i < 3; i++) {
            accounts.signUp(request);
        }
        assertThatThrownBy(() -> accounts.signUp(request)).isInstanceOf(BadRequestException.class);
    }
}
