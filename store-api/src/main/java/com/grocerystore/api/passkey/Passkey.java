package com.grocerystore.api.passkey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A passkey a customer registered. Ids and the public key are stored as base64url text. */
@Entity
@Table(name = "passkeys")
public class Passkey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "credential_id")
    private String credentialId;
    @Column(name = "public_key")
    private String publicKey;
    @Column(name = "signature_count")
    private long signatureCount;
    private String label;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected Passkey() {
    }

    public Passkey(Long customerId, String credentialId, String publicKey, long signatureCount, String label,
            Instant createdAt) {
        this.customerId = customerId;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.signatureCount = signatureCount;
        this.label = label;
        this.createdAt = createdAt;
    }

    public void used(long signatureCount, Instant now) {
        this.signatureCount = signatureCount;
        this.lastUsedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
