package com.grocerystore.api.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    public static final String RECEIVE = "RECEIVE";
    public static final String SALE = "SALE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "batch_id")
    private Long batchId;
    private String type;
    // Positive when stock comes in, negative when it goes out.
    private int quantity;
    private String reference;
    @Column(name = "created_at")
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(Long batchId, String type, int quantity, String reference, Instant createdAt) {
        this.batchId = batchId;
        this.type = type;
        this.quantity = quantity;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public Long getBatchId() {
        return batchId;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReference() {
        return reference;
    }
}
