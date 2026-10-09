package com.grocerystore.api.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id")
    private Long customerId;
    // Only a hash of the token is stored, so a copy of this table cannot be used to sign in.
    @Column(name = "token_hash")
    private String tokenHash;
    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Session() {
    }

    public Session(Long customerId, String tokenHash, Instant expiresAt) {
        this.customerId = customerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
