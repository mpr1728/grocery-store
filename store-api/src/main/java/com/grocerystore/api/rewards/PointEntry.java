package com.grocerystore.api.rewards;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_entries")
public class PointEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "customer_id")
    private Long customerId;
    private int points;
    private String reason;
    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "created_at")
    private Instant createdAt;

    protected PointEntry() {
    }

    public PointEntry(Long customerId, int points, String reason, String orderNumber, Instant createdAt) {
        this.customerId = customerId;
        this.points = points;
        this.reason = reason;
        this.orderNumber = orderNumber;
        this.createdAt = createdAt;
    }

    public int getPoints() {
        return points;
    }

    public String getReason() {
        return reason;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
