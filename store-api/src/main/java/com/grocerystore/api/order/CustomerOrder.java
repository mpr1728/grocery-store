package com.grocerystore.api.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "hub_id")
    private Long hubId;
    @Column(name = "customer_id")
    private Long customerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Column(name = "contact_phone")
    private String contactPhone;
    @Column(name = "delivery_address")
    private String deliveryAddress;
    @Column(name = "item_total_paise")
    private int itemTotalPaise;
    @Column(name = "delivery_fee_paise")
    private int deliveryFeePaise;
    @Column(name = "total_paise")
    private int totalPaise;
    @Column(name = "points_redeemed")
    private int pointsRedeemed;
    @Column(name = "points_discount_paise")
    private int pointsDiscountPaise;
    @Column(name = "points_earned")
    private int pointsEarned;
    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String orderNumber, Long hubId, Long customerId, String contactPhone,
            String deliveryAddress,
            List<OrderLine> lines, int deliveryFeePaise, int pointsRedeemed, int pointsDiscountPaise,
            int pointsEarned, Instant createdAt) {
        this.orderNumber = orderNumber;
        this.hubId = hubId;
        this.customerId = customerId;
        this.status = OrderStatus.PLACED;
        this.contactPhone = contactPhone;
        this.deliveryAddress = deliveryAddress;
        this.lines.addAll(lines);
        this.itemTotalPaise = lines.stream().mapToInt(OrderLine::getLineTotalPaise).reduce(0, Math::addExact);
        this.deliveryFeePaise = deliveryFeePaise;
        this.pointsRedeemed = pointsRedeemed;
        this.pointsDiscountPaise = pointsDiscountPaise;
        this.pointsEarned = pointsEarned;
        this.totalPaise = Math.addExact(itemTotalPaise, deliveryFeePaise) - pointsDiscountPaise;
        this.createdAt = createdAt;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getHubId() {
        return hubId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public int getItemTotalPaise() {
        return itemTotalPaise;
    }

    public int getDeliveryFeePaise() {
        return deliveryFeePaise;
    }

    public int getTotalPaise() {
        return totalPaise;
    }

    public int getPointsRedeemed() {
        return pointsRedeemed;
    }

    public int getPointsDiscountPaise() {
        return pointsDiscountPaise;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
