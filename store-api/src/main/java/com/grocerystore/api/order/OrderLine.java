package com.grocerystore.api.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id")
    private Long productId;
    // Name, unit, and price are copied at order time so later catalog changes do not rewrite history.
    @Column(name = "product_name")
    private String productName;
    @Column(name = "unit_label")
    private String unitLabel;
    @Column(name = "unit_price_paise")
    private int unitPricePaise;
    private int quantity;
    @Column(name = "line_total_paise")
    private int lineTotalPaise;

    protected OrderLine() {
    }

    public OrderLine(Long productId, String productName, String unitLabel, int unitPricePaise, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitLabel = unitLabel;
        this.unitPricePaise = unitPricePaise;
        this.quantity = quantity;
        this.lineTotalPaise = Math.multiplyExact(unitPricePaise, quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public int getUnitPricePaise() {
        return unitPricePaise;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getLineTotalPaise() {
        return lineTotalPaise;
    }
}
