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
import com.grocerystore.api.account.AccountDtos.PointView;
import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignUpResponse;
import com.grocerystore.api.account.AccountDtos.SignedIn;
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
    private final ObjectProvider<Mailer> mailer;
    private final RewardsService rewards;
    private final StoreProperties properties;
    private final Clock clock;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AccountService(CustomerRepository customers, SessionRepository sessions,
            SignupRequestRepository signups, ObjectProvider<Mailer> mailer, RewardsService rewards,
            StoreProperties properties, Clock clock) {
        this.customers = customers;
        this.sessions = sessions;
        this.signups = signups;
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
        if (customer == null || !customer.hasAccount()
                || !passwords.matches(request.password(), customer.getPasswordHash())) {
            throw new BadRequestException("Those details do not match an account.");
        }
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
