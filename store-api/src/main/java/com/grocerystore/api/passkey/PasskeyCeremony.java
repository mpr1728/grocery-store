package com.grocerystore.api.passkey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The options given to the browser for one passkey registration or sign-in, kept until it answers. Lambda may run
 * several copies of the API, so this cannot live in memory. Each one is used once.
 */
@Entity
@Table(name = "passkey_ceremonies")
public class PasskeyCeremony {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash")
    private String tokenHash;
    /** Set for a registration (the signed-in customer), empty for a sign-in. */
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "request_json")
    private String requestJson;
    @Column(name = "expires_at")
    private Instant expiresAt;

    protected PasskeyCeremony() {
    }

    public PasskeyCeremony(String tokenHash, Long customerId, String requestJson, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.customerId = customerId;
        this.requestJson = requestJson;
        this.expiresAt = expiresAt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
