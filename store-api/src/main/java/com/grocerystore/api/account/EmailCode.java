package com.grocerystore.api.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A one-time code sent to an email address, for signing in or signing up without a password. */
@Entity
@Table(name = "email_codes")
public class EmailCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash")
    private String tokenHash;
    private String email;
    @Column(name = "code_hash")
    private String codeHash;
    private int attempts;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at")
    private Instant createdAt;

    protected EmailCode() {
    }

    public EmailCode(String tokenHash, String email, String codeHash, Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
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

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    /** The address is proven. The customer gets a fresh window to enter their name. */
    public void verify(Instant now, Instant newExpiry) {
        this.verifiedAt = now;
        this.expiresAt = newExpiry;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
