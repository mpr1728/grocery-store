package com.grocerystore.api.account;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

/** ID tokens from Sign in with Apple. The client id is the Services ID made in the Apple developer account. */
@Component
public class AppleTokens extends ProviderTokens {

    @Autowired
    public AppleTokens(@Value("${store.apple-client-id}") String clientId) throws MalformedURLException {
        this(clientId, JWKSourceBuilder.<SecurityContext>create(
                URI.create("https://appleid.apple.com/auth/keys").toURL()).build());
    }

    public AppleTokens(String clientId, JWKSource<SecurityContext> keys) {
        super("Apple", clientId, Set.of("https://appleid.apple.com"), keys);
    }
}
