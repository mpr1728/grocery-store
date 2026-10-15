package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.account.AccountDtos.EmailCodeSent;
import com.grocerystore.api.account.AccountDtos.FinishEmailSignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountDtos.VerifyEmailCodeRequest;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.account.GoogleTokens;
import com.grocerystore.api.account.Mailer;
import com.grocerystore.api.common.BadRequestException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Google sign-in, with a test key standing in for Google's. */
@SpringBootTest
@ActiveProfiles("test")
class GoogleSignInTest {

    static final String CLIENT_ID = "test-client.apps.googleusercontent.com";
    static final RSAKey GOOGLE_KEY;
    static final RSAKey OTHER_KEY;

    static {
        try {
            GOOGLE_KEY = new RSAKeyGenerator(2048).keyID("google").generate();
            OTHER_KEY = new RSAKeyGenerator(2048).keyID("google").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class LastCode implements Mailer {
        String text;

        @Override
        public void send(String to, String subject, String text) {
            this.text = text;
        }

        String code() {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b[0-9]{6}\\b").matcher(text);
            m.find();
            return m.group();
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        GoogleTokens testGoogleTokens() {
            return new GoogleTokens(CLIENT_ID, new ImmutableJWKSet<>(new JWKSet(GOOGLE_KEY.toPublicJWK())));
        }

        @Bean
        LastCode lastCode() {
            return new LastCode();
        }
    }

    @Autowired
    AccountService accounts;
    @Autowired
    LastCode mailer;

    private static String token(RSAKey key, String subject, String email, boolean verified, String audience,
            String issuer, long expiresInSeconds) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).subject(subject).audience(audience)
                .claim("email", email).claim("email_verified", verified).claim("name", "Priya Shah")
                .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + expiresInSeconds * 1000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private static String good(String subject, String email) throws Exception {
        return token(GOOGLE_KEY, subject, email, true, CLIENT_ID, "https://accounts.google.com", 600);
    }

    @Test
    void firstGoogleSignInMakesAnAccountAndTheNextOneFindsIt() throws Exception {
        String subject = "g-" + System.nanoTime();
        String email = subject + "@gmail.com";
        SignedIn first = accounts.signInWithGoogle(good(subject, email));
        assertThat(first.account().name()).isEqualTo("Priya Shah");
        assertThat(first.account().email()).isEqualTo(email);
        assertThat(first.account().pointsBalance()).isEqualTo(50);

        // Same Google person, even after changing the email on their Google account.
        SignedIn again = accounts.signInWithGoogle(good(subject, "renamed" + email));
        assertThat(again.account().email()).isEqualTo(email);
        assertThat(again.account().pointsBalance()).isEqualTo(50);
    }

    @Test
    void anEmailCodeAccountIsLinkedNotDuplicated() throws Exception {
        String email = "linked" + System.nanoTime() + "@gmail.com";
        EmailCodeSent sent = accounts.sendEmailCode(email);
        accounts.verifyEmailCode(new VerifyEmailCodeRequest(sent.codeId(), mailer.code()));
        accounts.finishEmailSignUp(new FinishEmailSignUpRequest(sent.codeId(), "Arjun", null));

        SignedIn viaGoogle = accounts.signInWithGoogle(good("g-linked-" + System.nanoTime(), email));
        assertThat(viaGoogle.account().name()).isEqualTo("Arjun");
    }

    @Test
    void badTokensAreRefused() throws Exception {
        String subject = "g-bad-" + System.nanoTime();
        String email = subject + "@gmail.com";
        // Signed by someone other than Google.
        assertThatThrownBy(() -> accounts.signInWithGoogle(token(OTHER_KEY, subject, email, true, CLIENT_ID, "https://accounts.google.com", 600)))
                .isInstanceOf(BadRequestException.class);
        // Made for a different website's client id.
        assertThatThrownBy(() -> accounts.signInWithGoogle(token(GOOGLE_KEY, subject, email, true, "someone-else", "https://accounts.google.com", 600)))
                .isInstanceOf(BadRequestException.class);
        // Expired.
        assertThatThrownBy(() -> accounts.signInWithGoogle(token(GOOGLE_KEY, subject, email, true, CLIENT_ID, "https://accounts.google.com", -600)))
                .isInstanceOf(BadRequestException.class);
        // Not issued by Google.
        assertThatThrownBy(() -> accounts.signInWithGoogle(token(GOOGLE_KEY, subject, email, true, CLIENT_ID, "https://evil.example", 600)))
                .isInstanceOf(BadRequestException.class);
        // Email not confirmed by Google.
        assertThatThrownBy(() -> accounts.signInWithGoogle(token(GOOGLE_KEY, subject, email, false, CLIENT_ID, "https://accounts.google.com", 600)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("not confirmed");
        assertThatThrownBy(() -> accounts.signInWithGoogle("not a token")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void theButtonGetsTheClientId() {
        assertThat(accounts.googleClientId()).isEqualTo(CLIENT_ID);
    }
}
