package com.grocerystore.api.fashion;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A customer's saved measurements, in whole centimetres. */
@Entity
@Table(name = "fashion_measurements")
public class Measurements {

    @Id
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "bust_cm")
    private int bustCm;
    @Column(name = "waist_cm")
    private int waistCm;
    @Column(name = "hip_cm")
    private int hipCm;
    @Column(name = "shoulder_cm")
    private int shoulderCm;
    @Column(name = "height_cm")
    private int heightCm;
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Measurements() {
    }

    public Measurements(Long customerId) {
        this.customerId = customerId;
    }

    public void update(int bustCm, int waistCm, int hipCm, int shoulderCm, int heightCm, Instant now) {
        this.bustCm = bustCm;
        this.waistCm = waistCm;
        this.hipCm = hipCm;
        this.shoulderCm = shoulderCm;
        this.heightCm = heightCm;
        this.updatedAt = now;
    }

    public int getBustCm() {
        return bustCm;
    }

    public int getWaistCm() {
        return waistCm;
    }

    public int getHipCm() {
        return hipCm;
    }

    public int getShoulderCm() {
        return shoulderCm;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
