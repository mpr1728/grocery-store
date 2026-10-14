package com.grocerystore.api.fashion;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One made-to-order garment. The design, measurements, and prices are copies taken when it was ordered. */
@Entity
@Table(name = "fashion_orders")
public class FashionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number")
    private String orderNumber;
    @Column(name = "customer_id")
    private Long customerId;
    @Column(name = "garment_code")
    private String garmentCode;
    @Column(name = "garment_name")
    private String garmentName;
    private String neckline;
    private String sleeve;
    @Column(name = "garment_length")
    private String length;
    private String fit;
    private String fabric;
    private String colour;
    @Column(name = "colour_hex")
    private String colourHex;
    /** The chosen codes as garment/neckline/sleeve/length/fit/fabric/colour, so the studio can draw the design again. */
    @Column(name = "design_codes")
    private String designCodes;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "body_shape")
    private BodyShape bodyShape;
    @Column(name = "base_price_paise")
    private int basePricePaise;
    @Column(name = "extras_paise")
    private int extrasPaise;
    @Column(name = "shipping_paise")
    private int shippingPaise;
    @Column(name = "total_paise")
    private int totalPaise;
    @Column(name = "contact_phone")
    private String contactPhone;
    @Column(name = "delivery_address")
    private String deliveryAddress;
    private String notes;
    @Enumerated(EnumType.STRING)
    private FashionOrderStatus status;
    @Column(name = "placed_at")
    private Instant placedAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "ready_by")
    private LocalDate readyBy;

    protected FashionOrder() {
    }

    public FashionOrder(String orderNumber, Long customerId, FashionQuote quote, int bustCm, int waistCm, int hipCm,
            int shoulderCm, int heightCm, String contactPhone, String deliveryAddress, String notes, Instant now) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.garmentCode = quote.garment().getCode();
        this.garmentName = quote.garment().getName();
        this.neckline = quote.neckline().getLabel();
        this.sleeve = quote.sleeve().getLabel();
        this.length = quote.length().getLabel();
        this.fit = quote.fit().getLabel();
        this.fabric = quote.fabric().getName();
        this.colour = quote.colour().getName();
        this.colourHex = quote.colour().getHex();
        this.designCodes = String.join("/", quote.garment().getCode(), quote.neckline().getCode(), quote.sleeve().getCode(),
                quote.length().getCode(), quote.fit().getCode(), quote.fabric().getCode(), quote.colour().getCode());
        this.bustCm = bustCm;
        this.waistCm = waistCm;
        this.hipCm = hipCm;
        this.shoulderCm = shoulderCm;
        this.heightCm = heightCm;
        this.bodyShape = BodyShape.of(bustCm, waistCm, hipCm);
        this.basePricePaise = quote.garment().getBasePricePaise();
        this.extrasPaise = quote.extrasPaise();
        this.shippingPaise = quote.shippingPaise();
        this.totalPaise = quote.totalPaise();
        this.contactPhone = contactPhone;
        this.deliveryAddress = deliveryAddress;
        this.notes = notes;
        this.status = FashionOrderStatus.PLACED;
        this.placedAt = now;
        this.updatedAt = now;
        this.readyBy = quote.readyBy();
    }

    public void moveTo(FashionOrderStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getGarmentName() {
        return garmentName;
    }

    public String getNeckline() {
        return neckline;
    }

    public String getSleeve() {
        return sleeve;
    }

    public String getLength() {
        return length;
    }

    public String getFit() {
        return fit;
    }

    public String getFabric() {
        return fabric;
    }

    public String getColour() {
        return colour;
    }

    public String getColourHex() {
        return colourHex;
    }

    public String getDesignCodes() {
        return designCodes;
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

    public BodyShape getBodyShape() {
        return bodyShape;
    }

    public int getBasePricePaise() {
        return basePricePaise;
    }

    public int getExtrasPaise() {
        return extrasPaise;
    }

    public int getShippingPaise() {
        return shippingPaise;
    }

    public int getTotalPaise() {
        return totalPaise;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getNotes() {
        return notes;
    }

    public FashionOrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public LocalDate getReadyBy() {
        return readyBy;
    }
}
