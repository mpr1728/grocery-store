package com.grocerystore.api.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.account.AccountDtos.AccountView;
import com.grocerystore.api.account.AccountDtos.ConfirmSignUpRequest;
import com.grocerystore.api.account.AccountDtos.EmailCodeSent;
import com.grocerystore.api.account.AccountDtos.EmailVerified;
import com.grocerystore.api.account.AccountDtos.FinishEmailSignUpRequest;
import com.grocerystore.api.account.AccountDtos.PointView;
import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignUpResponse;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountDtos.VerifyEmailCodeRequest;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.PhoneNumbers;
import com.grocerystore.api.common.StoreProperties;
import com.grocerystore.api.common.UnauthorizedException;
import com.grocerystore.api.common.UnavailableException;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;
import com.grocerystore.api.rewards.RewardsService;

@Service
public class AccountService {

    private static final Duration SESSION_LENGTH = Duration.ofDays(30);
    private static final String BEARER = "Bearer ";
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(10);
    private static final Duration CODE_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_CODES_PER_WINDOW = 3;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final CustomerRepository customers;
    private final SessionRepository sessions;
    private final SignupRequestRepository signups;
    private final EmailCodeRepository emailCodes;
    private final GoogleTokens google;
    private final AppleTokens apple;
    private final ObjectProvider<Mailer> mailer;
    private final RewardsService rewards;
    private final StoreProperties properties;
    private final Clock clock;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AccountService(CustomerRepository customers, SessionRepository sessions,
            SignupRequestRepository signups, EmailCodeRepository emailCodes, GoogleTokens google,
            AppleTokens apple, ObjectProvider<Mailer> mailer, RewardsService rewards, StoreProperties properties, Clock clock) {
        this.customers = customers;
        this.sessions = sessions;
        this.signups = signups;
        this.emailCodes = emailCodes;
        this.google = google;
        this.apple = apple;
        this.mailer = mailer;
        this.rewards = rewards;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Starts a sign-up. Where email can be sent, a 6 digit code goes to the address and the account is
     * created by confirmSignUp. Where it cannot (local runs), the account is created straight away.
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        Instant now = Instant.now(clock);
        String name = request.name().trim();
        String phone = PhoneNumbers.normalize(request.phone());
        String email = normalizeEmail(request.email());
        requireFree(phone, email);
        String passwordHash = passwords.encode(request.password());

        Mailer sender = mailer.getIfAvailable();
        if (sender == null) {
            SignedIn signedIn = createAccount(name, phone, email, passwordHash, now);
            return new SignUpResponse(signedIn.token(), signedIn.account(), null, null);
        }

        if (signups.countByEmailAndCreatedAtAfter(email, now.minus(CODE_WINDOW)) >= MAX_CODES_PER_WINDOW) {
            throw new BadRequestException("Too many codes were sent to this address. Please wait a few minutes.");
        }
        String signupId = randomToken();
        String code = String.format("%06d", random.nextInt(1_000_000));
        signups.save(new SignupRequest(hash(signupId), name, phone, email, passwordHash,
                hash(signupId + code), now.plus(CODE_LIFETIME), now));
        try {
            sender.send(email, code + " is your " + properties.brandName() + " code",
                    "Your " + properties.brandName() + " sign-up code is " + code + ".\n\n"
                            + "It works for " + CODE_LIFETIME.toMinutes() + " minutes. "
                            + "If you did not ask for it, you can ignore this email.\n\n"
                            + properties.brandName() + "\n" + properties.brandTagline());
        } catch (RuntimeException e) {
            log.warn("Sign-up code could not be sent", e);
            throw new UnavailableException("We could not send a code to that email address. Check it and try again.");
        }
        return new SignUpResponse(null, null, signupId, email);
    }

    // A wrong code must still be counted, so that error does not undo the transaction.
    @Transactional(noRollbackFor = BadRequestException.class)
    public SignedIn confirmSignUp(ConfirmSignUpRequest request) {
        Instant now = Instant.now(clock);
        SignupRequest signup = signups.findByTokenHash(hash(request.signupId()))
                .filter(s -> s.getExpiresAt().isAfter(now) && s.getAttempts() < MAX_CODE_ATTEMPTS)
                .orElseThrow(() -> new BadRequestException("That code is no longer valid. Please start again."));
        boolean matches = MessageDigest.isEqual(
                hash(request.signupId() + request.code()).getBytes(StandardCharsets.UTF_8),
                signup.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            signup.addAttempt();
            throw new BadRequestException("That code is not right. Check the email and try again.");
        }
        requireFree(signup.getPhone(), signup.getEmail());
        SignedIn signedIn = createAccount(signup.getName(), signup.getPhone(), signup.getEmail(),
                signup.getPasswordHash(), now);
        signups.delete(signup);
        return signedIn;
    }

    @Transactional
    public SignedIn signIn(SignInRequest request) {
        String login = request.login().trim();
        Customer customer = (login.contains("@")
                ? customers.findByEmail(normalizeEmail(login))
                : customers.findByPhone(PhoneNumbers.normalize(login))).orElse(null);
        if (customer == null || customer.getPasswordHash() == null
                || !passwords.matches(request.password(), customer.getPasswordHash())) {
            throw new BadRequestException("Those details do not match an account.");
        }
        return new SignedIn(newSession(customer, Instant.now(clock)), view(customer));
    }

    /**
     * Sends a one-time code to an email address. The same code signs in an existing account or starts a new one,
     * so there is no password to forget. Where no email can be sent (local runs), the code goes to the log.
     */
    @Transactional
    public EmailCodeSent sendEmailCode(String rawEmail) {
        Instant now = Instant.now(clock);
        String email = normalizeEmail(rawEmail);
        if (emailCodes.countByEmailAndCreatedAtAfter(email, now.minus(CODE_WINDOW)) >= MAX_CODES_PER_WINDOW) {
            throw new BadRequestException("Too many codes were sent to this address. Please wait a few minutes.");
        }
        String codeId = randomToken();
        String code = String.format("%06d", random.nextInt(1_000_000));
        emailCodes.save(new EmailCode(hash(codeId), email, hash(codeId + code), now.plus(CODE_LIFETIME), now));
        Mailer sender = mailer.getIfAvailable();
        if (sender == null) {
            log.warn("No mail sender is set up, so the sign-in code is only logged: {}", code);
            return new EmailCodeSent(codeId, email);
        }
        try {
            sender.send(email, code + " is your " + properties.brandName() + " code",
                    "Your " + properties.brandName() + " code is " + code + ".\n\n"
                            + "Enter it to sign in or to create your account. It works for "
                            + CODE_LIFETIME.toMinutes() + " minutes. "
                            + "If you did not ask for it, you can ignore this email.\n\n"
                            + properties.brandName() + "\n" + properties.brandTagline());
        } catch (RuntimeException e) {
            log.warn("Sign-in code could not be sent", e);
            throw new UnavailableException("We could not send a code to that email address. Check it and try again.");
        }
        return new EmailCodeSent(codeId, email);
    }

    /** Checks the code. An existing account is signed in; a new address is asked for a name next. */
    @Transactional(noRollbackFor = BadRequestException.class)
    public EmailVerified verifyEmailCode(VerifyEmailCodeRequest request) {
        Instant now = Instant.now(clock);
        EmailCode emailCode = emailCodes.findByTokenHash(hash(request.codeId()))
                .filter(c -> c.getExpiresAt().isAfter(now) && c.getAttempts() < MAX_CODE_ATTEMPTS)
                .orElseThrow(() -> new BadRequestException("That code is no longer valid. Please ask for a new one."));
        boolean matches = MessageDigest.isEqual(
                hash(request.codeId() + request.code()).getBytes(StandardCharsets.UTF_8),
                emailCode.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            emailCode.addAttempt();
            throw new BadRequestException("That code is not right. Check the email and try again.");
        }
        Optional<Customer> existing = customers.findByEmail(emailCode.getEmail());
        if (existing.isPresent()) {
            emailCodes.delete(emailCode);
            return new EmailVerified(newSession(existing.get(), now), view(existing.get()), false);
        }
        emailCode.verify(now, now.plus(CODE_WINDOW));
        return new EmailVerified(null, null, true);
    }

    /** Creates the account for an address whose code was entered correctly. */
    @Transactional
    public SignedIn finishEmailSignUp(FinishEmailSignUpRequest request) {
        Instant now = Instant.now(clock);
        EmailCode emailCode = emailCodes.findByTokenHash(hash(request.codeId()))
                .filter(c -> c.getVerifiedAt() != null && c.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new BadRequestException("That sign-up has expired. Please ask for a new code."));
        String phone = PhoneNumbers.normalize(request.phone());
        requireFree(phone, emailCode.getEmail());
        SignedIn signedIn = createAccount(request.name().trim(), phone, emailCode.getEmail(), null, now);
        emailCodes.delete(emailCode);
        return signedIn;
    }

    /** Signs in with a Google ID token. */
    @Transactional
    public SignedIn signInWithGoogle(String idToken) {
        ProviderTokens.Identity identity = google.verify(idToken);
        return signInWithProvider(identity, identity.name(), customers.findByGoogleSubject(identity.subject()),
                Customer::setGoogleSubject);
    }

    /**
     * Signs in with an Apple ID token. Apple sends the person's name only to the page, and only the first time,
     * so the page passes it on; it is used only when a new account is made.
     */
    @Transactional
    public SignedIn signInWithApple(String idToken, String nameFromPage) {
        ProviderTokens.Identity identity = apple.verify(idToken);
        return signInWithProvider(identity, nameFromPage, customers.findByAppleSubject(identity.subject()),
                Customer::setAppleSubject);
    }

    /**
     * The provider has confirmed the email address, so an account with that address is the same person and is
     * linked; otherwise a new account is made.
     */
    private SignedIn signInWithProvider(ProviderTokens.Identity identity, String name, Optional<Customer> linked,
            java.util.function.BiConsumer<Customer, String> link) {
        Instant now = Instant.now(clock);
        if (linked.isPresent()) {
            return new SignedIn(newSession(linked.get(), now), view(linked.get()));
        }
        String email = normalizeEmail(identity.email());
        Optional<Customer> sameEmail = customers.findByEmail(email);
        if (sameEmail.isPresent()) {
            link.accept(sameEmail.get(), identity.subject());
            return new SignedIn(newSession(sameEmail.get(), now), view(sameEmail.get()));
        }
        String display = name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name.trim();
        SignedIn created = createAccount(display.length() > 100 ? display.substring(0, 100) : display, null, email,
                null, now);
        link.accept(customers.findByEmail(email).orElseThrow(), identity.subject());
        return created;
    }

    /** The Google client id for the sign-in button, or empty when Google sign-in is off. */
    public String googleClientId() {
        return google.clientId();
    }

    /** The Apple Services ID for the sign-in button, or empty when Sign in with Apple is off. */
    public String appleClientId() {
        return apple.clientId();
    }

    /** Starts a session for a customer proven some other way, such as a passkey. */
    public SignedIn startSession(Customer customer) {
        return new SignedIn(newSession(customer, Instant.now(clock)), view(customer));
    }

    private void requireFree(String phone, String email) {
        if (phone != null && customers.findByPhone(phone).filter(Customer::hasAccount).isPresent()) {
            throw new BadRequestException("This mobile number already has an account. Sign in instead.");
        }
        if (customers.findByEmail(email).isPresent()) {
            throw new BadRequestException("This email address already has an account. Sign in instead.");
        }
    }

    private SignedIn createAccount(String name, String phone, String email, String passwordHash, Instant now) {
        // A number that only placed guest orders becomes an account here. Mobile numbers are not
        // verified, so nothing private may hang off a guest record.
        Customer customer = phone == null ? null : customers.findByPhone(phone).orElse(null);
        if (customer == null) {
            customer = customers.save(new Customer(phone, name, now));
        } else {
            customer.setName(name);
        }
        customer.setEmail(email);
        customer.setPasswordHash(passwordHash);
        if (properties.rewardWelcomePoints() > 0) {
            rewards.credit(customer.getId(), properties.rewardWelcomePoints(), RewardsService.WELCOME, null, now);
        }
        return new SignedIn(newSession(customer, now), view(customer));
    }

    @Transactional
    public void signOut(String authorization) {
        token(authorization).ifPresent(token -> sessions.deleteByTokenHash(hash(token)));
    }

    /** The signed-in customer, or empty when the request carries no session. A bad or expired session is an error. */
    @Transactional(readOnly = true)
    public Optional<Customer> signedIn(String authorization) {
        Optional<String> token = token(authorization);
        if (token.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessions.findByTokenHash(hash(token.get()))
                .filter(s -> s.getExpiresAt().isAfter(Instant.now(clock)))
                .orElseThrow(() -> new UnauthorizedException("Your session has ended. Please sign in again."));
        return customers.findById(session.getCustomerId());
    }

    @Transactional(readOnly = true)
    public Customer require(String authorization) {
        return signedIn(authorization).orElseThrow(() -> new UnauthorizedException("Please sign in first."));
    }

    @Transactional(readOnly = true)
    public AccountView view(Customer customer) {
        return new AccountView(customer.getName(), customer.getPhone(), customer.getEmail(),
                customer.getPointsBalance(),
                rewards.recent(customer.getId()).stream()
                        .map(e -> new PointView(e.getPoints(), e.getReason(), e.getOrderNumber(), e.getCreatedAt()))
                        .toList());
    }

    private String newSession(Customer customer, Instant now) {
        String token = randomToken();
        sessions.save(new Session(customer.getId(), hash(token), now.plus(SESSION_LENGTH)));
        return token;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static Optional<String> token(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
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
