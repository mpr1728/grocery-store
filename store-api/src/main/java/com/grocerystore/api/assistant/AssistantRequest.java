package com.grocerystore.api.assistant;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistant_requests")
public class AssistantRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected AssistantRequest() {
    }

    public AssistantRequest(Long customerId, Instant createdAt) {
        this.customerId = customerId;
        this.createdAt = createdAt;
    }
}
