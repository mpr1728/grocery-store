package com.grocerystore.api.passkey;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;

/** Lets the WebAuthn library look up stored passkeys. The "username" is the account's email address. */
@Component
class PasskeyCredentials implements CredentialRepository {

    private final PasskeyRepository passkeys;
    private final CustomerRepository customers;

    PasskeyCredentials(PasskeyRepository passkeys, CustomerRepository customers) {
        this.passkeys = passkeys;
        this.customers = customers;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return customers.findByEmail(username)
                .map(c -> passkeys.findByCustomerIdOrderById(c.getId()).stream()
                        .map(p -> PublicKeyCredentialDescriptor.builder().id(bytes(p.getCredentialId())).build())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return customers.findByEmail(username).map(Customer::getPasskeyHandle).map(PasskeyCredentials::bytes);
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return customers.findByPasskeyHandle(userHandle.getBase64Url()).map(Customer::getEmail);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return passkeys.findByCredentialId(credentialId.getBase64Url())
                .flatMap(p -> customers.findById(p.getCustomerId())
                        .filter(c -> userHandle.getBase64Url().equals(c.getPasskeyHandle()))
                        .map(c -> registered(p, c)));
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return passkeys.findByCredentialId(credentialId.getBase64Url())
                .flatMap(p -> customers.findById(p.getCustomerId()).map(c -> registered(p, c)))
                .map(Set::of)
                .orElse(Set.of());
    }

    private static RegisteredCredential registered(Passkey p, Customer c) {
        return RegisteredCredential.builder()
                .credentialId(bytes(p.getCredentialId()))
                .userHandle(bytes(c.getPasskeyHandle()))
                .publicKeyCose(bytes(p.getPublicKey()))
                .signatureCount(p.getSignatureCount())
                .build();
    }

    static ByteArray bytes(String base64Url) {
        try {
            return ByteArray.fromBase64Url(base64Url);
        } catch (Base64UrlException e) {
            throw new IllegalStateException("Stored passkey data is not base64url", e);
        }
    }
}
