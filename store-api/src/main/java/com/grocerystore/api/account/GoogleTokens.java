package com.grocerystore.api.account;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.grocerystore.api.common.BadRequestException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * Checks the ID token Google's "Sign in with Google" button gives the browser: signed by Google, issued for our
 * client id, not expired, and for a verified email address. An empty client id switches Google sign-in off.
 */
@Component
public class GoogleTokens {

    public record GoogleIdentity(String subject, String email, String name) {
    }

    private static final String GOOGLE_KEYS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final Logger log = LoggerFactory.getLogger(GoogleTokens.class);

    private final String clientId;
    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

    @Autowired
    public GoogleTokens(@Value("${store.google-client-id}") String clientId) throws MalformedURLException {
        // Google's public keys, fetched when first needed and cached; they rotate every few weeks.
        this(clientId, JWKSourceBuilder.<SecurityContext>create(URI.create(GOOGLE_KEYS).toURL()).build());
    }

    public GoogleTokens(String clientId, JWKSource<SecurityContext> keys) {
        this.clientId = clientId == null ? "" : clientId.trim();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(this.clientId, null,
                Set.of("iss", "sub", "email", "exp", "iat")));
    }

    public boolean enabled() {
        return !clientId.isEmpty();
    }

    public String clientId() {
        return clientId;
    }

    public GoogleIdentity verify(String idToken) {
        if (!enabled()) {
            throw new BadRequestException("Google sign-in is not switched on.");
        }
        JWTClaimsSet claims;
        try {
            claims = processor.process(idToken, null);
        } catch (ParseException | BadJOSEException | JOSEException e) {
            log.info("Google token refused: {}", e.getMessage());
            throw new BadRequestException("Google sign-in could not be checked. Please try again.");
        }
        try {
            if (!GOOGLE_ISSUERS.contains(claims.getIssuer())) {
                throw new BadRequestException("Google sign-in could not be checked. Please try again.");
            }
            if (!Boolean.TRUE.equals(claims.getBooleanClaim("email_verified"))) {
                throw new BadRequestException("Google has not confirmed that email address. Use an email code instead.");
            }
            String name = claims.getStringClaim("name");
            return new GoogleIdentity(claims.getSubject(), claims.getStringClaim("email"), name);
        } catch (ParseException e) {
            throw new BadRequestException("Google sign-in could not be checked. Please try again.");
        }
    }
}
