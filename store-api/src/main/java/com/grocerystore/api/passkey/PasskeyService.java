package com.grocerystore.api.passkey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyOptions;
import com.grocerystore.api.passkey.PasskeyDtos.CeremonyResponse;
import com.grocerystore.api.passkey.PasskeyDtos.PasskeyView;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

/**
 * Passkeys: a signed-in customer adds one, and later signs in with it without typing anything. A passkey belongs
 * to the web address it was made on (store.passkey-rp-id), so passkeys made on the dev site will not work once the
 * store moves to its own domain.
 */
@Service
public class PasskeyService {

    private static final Duration CEREMONY_LIFETIME = Duration.ofMinutes(5);
    private static final int MAX_PASSKEYS = 10;
    private static final Logger log = LoggerFactory.getLogger(PasskeyService.class);

    private final PasskeyRepository passkeys;
    private final PasskeyCeremonyRepository ceremonies;
    private final CustomerRepository customers;
    private final AccountService accounts;
    private final Clock clock;
    private final RelyingParty relyingParty;
    private final SecureRandom random = new SecureRandom();

    public PasskeyService(PasskeyRepository passkeys, PasskeyCeremonyRepository ceremonies,
            CustomerRepository customers, AccountService accounts, PasskeyCredentials credentials, Clock clock,
            @Value("${store.passkey-rp-id}") String rpId, @Value("${store.passkey-rp-name}") String rpName,
            @Value("${store.passkey-origins}") String origins) {
        this.passkeys = passkeys;
        this.ceremonies = ceremonies;
        this.customers = customers;
        this.accounts = accounts;
        this.clock = clock;
        Set<String> allowed = Arrays.stream(origins.split(",")).map(String::trim).filter(o -> !o.isEmpty())
                .collect(Collectors.toSet());
        this.relyingParty = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder().id(rpId).name(rpName).build())
                .credentialRepository(credentials)
                .origins(allowed)
                .build();
    }

    @Transactional
    public CeremonyOptions startRegistration(Long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        if (passkeys.findByCustomerIdOrderById(customerId).size() >= MAX_PASSKEYS) {
            throw new BadRequestException("You already have " + MAX_PASSKEYS + " passkeys. Remove one to add another.");
        }
        if (customer.getPasskeyHandle() == null) {
            customer.setPasskeyHandle(new ByteArray(randomBytes()).getBase64Url());
        }
        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                        .name(customer.getEmail())
                        .displayName(customer.getName())
                        .id(PasskeyCredentials.bytes(customer.getPasskeyHandle()))
                        .build())
                // Discoverable, so sign-in needs no email typed; verified, so the phone's lock is checked.
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build())
                .build());
        try {
            return save(customerId, options.toJson(), options.toCredentialsCreateJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    // A refused answer still uses up the options, so that error does not undo the transaction.
    @Transactional(noRollbackFor = BadRequestException.class)
    public PasskeyView finishRegistration(Long customerId, CeremonyResponse response) {
        PasskeyCeremony ceremony = take(response.ceremonyId());
        if (!customerId.equals(ceremony.getCustomerId())) {
            throw expired();
        }
        RegistrationResult result;
        try {
            result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(PublicKeyCredentialCreationOptions.fromJson(ceremony.getRequestJson()))
                    .response(PublicKeyCredential.parseRegistrationResponseJson(response.credentialJson()))
                    .build());
        } catch (RegistrationFailedException | IOException e) {
            log.info("Passkey registration refused: {}", e.getMessage());
            throw new BadRequestException("That passkey could not be added. Please try again.");
        }
        String label = response.label() == null || response.label().isBlank() ? "Passkey" : response.label().trim();
        Passkey saved = passkeys.save(new Passkey(customerId, result.getKeyId().getId().getBase64Url(),
                result.getPublicKeyCose().getBase64Url(), result.getSignatureCount(), label, Instant.now(clock)));
        return view(saved);
    }

    @Transactional
    public CeremonyOptions startSignIn() {
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build());
        try {
            return save(null, request.toJson(), request.toCredentialsGetJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    // A refused answer still uses up the options, so that error does not undo the transaction.
    @Transactional(noRollbackFor = BadRequestException.class)
    public SignedIn finishSignIn(CeremonyResponse response) {
        PasskeyCeremony ceremony = take(response.ceremonyId());
        if (ceremony.getCustomerId() != null) {
            throw expired();
        }
        AssertionResult result;
        try {
            result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(AssertionRequest.fromJson(ceremony.getRequestJson()))
                    .response(PublicKeyCredential.parseAssertionResponseJson(response.credentialJson()))
                    .build());
        } catch (AssertionFailedException | IOException e) {
            log.info("Passkey sign-in refused: {}", e.getMessage());
            throw new BadRequestException("That passkey is not recognised here. Sign in with an email code instead.");
        }
        if (!result.isSuccess()) {
            throw new BadRequestException("That passkey could not be checked. Please try again.");
        }
        Passkey passkey = passkeys.findByCredentialId(result.getCredentialId().getBase64Url()).orElseThrow();
        passkey.used(result.getSignatureCount(), Instant.now(clock));
        return accounts.startSession(customers.findById(passkey.getCustomerId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<PasskeyView> list(Long customerId) {
        return passkeys.findByCustomerIdOrderById(customerId).stream().map(PasskeyService::view).toList();
    }

    @Transactional
    public void remove(Long customerId, Long passkeyId) {
        Passkey passkey = passkeys.findById(passkeyId).filter(p -> p.getCustomerId().equals(customerId))
                .orElseThrow(() -> new NotFoundException("Passkey " + passkeyId + " was not found"));
        passkeys.delete(passkey);
    }

    private CeremonyOptions save(Long customerId, String requestJson, String browserJson) {
        String ceremonyId = new ByteArray(randomBytes()).getBase64Url();
        ceremonies.save(new PasskeyCeremony(hash(ceremonyId), customerId, requestJson,
                Instant.now(clock).plus(CEREMONY_LIFETIME)));
        return new CeremonyOptions(ceremonyId, browserJson);
    }

    /** Each set of options can be answered once. */
    private PasskeyCeremony take(String ceremonyId) {
        PasskeyCeremony ceremony = ceremonies.findByTokenHash(hash(ceremonyId)).orElseThrow(PasskeyService::expired);
        ceremonies.delete(ceremony);
        if (!ceremony.getExpiresAt().isAfter(Instant.now(clock))) {
            throw expired();
        }
        return ceremony;
    }

    private static BadRequestException expired() {
        return new BadRequestException("That passkey request has expired. Please try again.");
    }

    private static PasskeyView view(Passkey p) {
        return new PasskeyView(p.getId(), p.getLabel(), p.getCreatedAt(), p.getLastUsedAt());
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytes;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
