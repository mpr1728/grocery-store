package com.grocerystore.api.customer;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phone;
    private String name;
    private String email;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "points_balance")
    private int pointsBalance;

    protected Customer() {
    }

    public Customer(String phone, String name, Instant createdAt) {
        this.phone = phone;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** A guest who has only ordered has no password yet. */
    public boolean hasAccount() {
        return passwordHash != null;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public void addPoints(int points) {
        this.pointsBalance = Math.addExact(pointsBalance, points);
    }
}
