package com.grocerystore.api.account;

import java.text.ParseException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grocerystore.api.common.BadRequestException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

/**
 * Checks an ID token from a sign-in provider (Google, Apple): signed with the provider's published keys, issued
 * by the provider for our client id, not expired, and for an email address the provider has confirmed. An empty
 * client id switches that provider off.
 */
public abstract class ProviderTokens {

    /** Who the provider says signed in. name may be empty: Apple never puts it in the token. */
    public record Identity(String subject, String email, String name) {
    }

    private static final Logger log = LoggerFactory.getLogger(ProviderTokens.class);

    private final String provider;
    private final String clientId;
    private final Set<String> issuers;
    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

    protected ProviderTokens(String provider, String clientId, Set<String> issuers, JWKSource<SecurityContext> keys) {
        this.provider = provider;
        this.clientId = clientId == null ? "" : clientId.trim();
        this.issuers = issuers;
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

    public Identity verify(String idToken) {
        if (!enabled()) {
            throw new BadRequestException(provider + " sign-in is not switched on.");
        }
        BadRequestException refused = new BadRequestException(provider + " sign-in could not be checked. Please try again.");
        JWTClaimsSet claims;
        try {
            claims = processor.process(idToken, null);
        } catch (ParseException | BadJOSEException | JOSEException e) {
            log.info("{} token refused: {}", provider, e.getMessage());
            throw refused;
        }
        if (!issuers.contains(claims.getIssuer())) {
            throw refused;
        }
        // Google sends true; Apple has sent both true and "true".
        Object verified = claims.getClaim("email_verified");
        if (!Boolean.TRUE.equals(verified) && !"true".equals(verified)) {
            throw new BadRequestException(provider + " has not confirmed that email address. Use an email code instead.");
        }
        try {
            return new Identity(claims.getSubject(), claims.getStringClaim("email"), claims.getStringClaim("name"));
        } catch (ParseException e) {
            throw refused;
        }
    }
}
