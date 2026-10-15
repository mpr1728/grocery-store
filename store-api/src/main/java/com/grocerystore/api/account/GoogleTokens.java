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

/** ID tokens from Google's "Sign in with Google" button. */
@Component
public class GoogleTokens extends ProviderTokens {

    @Autowired
    public GoogleTokens(@Value("${store.google-client-id}") String clientId) throws MalformedURLException {
        // Google's public keys, fetched when first needed and cached; they rotate every few weeks.
        this(clientId, JWKSourceBuilder.<SecurityContext>create(
                URI.create("https://www.googleapis.com/oauth2/v3/certs").toURL()).build());
    }

    public GoogleTokens(String clientId, JWKSource<SecurityContext> keys) {
        super("Google", clientId, Set.of("accounts.google.com", "https://accounts.google.com"), keys);
    }
}
