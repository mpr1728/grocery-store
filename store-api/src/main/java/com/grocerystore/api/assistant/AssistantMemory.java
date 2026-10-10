package com.grocerystore.api.assistant;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One lasting fact a shopper told the assistant. */
@Entity
@Table(name = "assistant_memories")
public class AssistantMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id")
    private Long customerId;
    private String fact;
    @Column(name = "created_at")
    private Instant createdAt;

    protected AssistantMemory() {
    }

    public AssistantMemory(Long customerId, String fact, Instant createdAt) {
        this.customerId = customerId;
        this.fact = fact;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getFact() {
        return fact;
    }
}
