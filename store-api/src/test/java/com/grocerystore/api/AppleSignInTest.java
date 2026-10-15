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

import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.account.AppleTokens;
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

/** Sign in with Apple, with a test key standing in for Apple's. */
@SpringBootTest
@ActiveProfiles("test")
class AppleSignInTest {

    static final String SERVICES_ID = "com.panyaloka.web";
    static final RSAKey APPLE_KEY;

    static {
        try {
            APPLE_KEY = new RSAKeyGenerator(2048).keyID("apple").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        AppleTokens testAppleTokens() {
            return new AppleTokens(SERVICES_ID, new ImmutableJWKSet<>(new JWKSet(APPLE_KEY.toPublicJWK())));
        }
    }

    @Autowired
    AccountService accounts;

    // Apple puts no name in the token, and has sent email_verified as the text "true".
    private static String token(String subject, String email, Object verified, String issuer) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer).subject(subject).audience(SERVICES_ID)
                .claim("email", email).claim("email_verified", verified).claim("is_private_email", "true")
                .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 600_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("apple").build(), claims);
        jwt.sign(new RSASSASigner(APPLE_KEY));
        return jwt.serialize();
    }

    @Test
    void firstSignInUsesTheNameFromThePageAndLaterOnesFindTheSameAccount() throws Exception {
        String subject = "000123." + System.nanoTime();
        String relay = "x" + System.nanoTime() + "@privaterelay.appleid.com";
        SignedIn first = accounts.signInWithApple(token(subject, relay, "true", "https://appleid.apple.com"), "Anika Rao");
        assertThat(first.account().name()).isEqualTo("Anika Rao");
        assertThat(first.account().email()).isEqualTo(relay);

        // Apple sends no name after the first time; the account keeps the one it has.
        SignedIn again = accounts.signInWithApple(token(subject, relay, true, "https://appleid.apple.com"), null);
        assertThat(again.account().name()).isEqualTo("Anika Rao");
        assertThat(again.account().pointsBalance()).isEqualTo(50);
    }

    @Test
    void withoutANameTheAddressStandsIn() throws Exception {
        String email = "noname" + System.nanoTime() + "@icloud.com";
        SignedIn signedIn = accounts.signInWithApple(token("s" + System.nanoTime(), email, "true", "https://appleid.apple.com"), " ");
        assertThat(signedIn.account().name()).isEqualTo(email.substring(0, email.indexOf('@')));
    }

    @Test
    void tokensNotFromAppleOrUnconfirmedAreRefused() throws Exception {
        String subject = "s" + System.nanoTime();
        assertThatThrownBy(() -> accounts.signInWithApple(token(subject, subject + "@icloud.com", "true", "https://accounts.google.com"), null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accounts.signInWithApple(token(subject, subject + "@icloud.com", "false", "https://appleid.apple.com"), null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("not confirmed");
        assertThat(accounts.appleClientId()).isEqualTo(SERVICES_ID);
    }
}
