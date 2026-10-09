package com.grocerystore.api.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A sign-up that is waiting for the emailed code. */
@Entity
@Table(name = "signup_requests")
public class SignupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash")
    private String tokenHash;
    private String name;
    private String phone;
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "code_hash")
    private String codeHash;
    private int attempts;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at")
    private Instant createdAt;

    protected SignupRequest() {
    }

    public SignupRequest(String tokenHash, String name, String phone, String email, String passwordHash,
            String codeHash, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public void addAttempt() {
        attempts++;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
