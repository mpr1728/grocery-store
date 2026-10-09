package com.grocerystore.api.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hub_products")
public class HubProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hub_id")
    private Long hubId;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "price_paise")
    private int pricePaise;
    private boolean active = true;

    protected HubProduct() {
    }

    public HubProduct(Long hubId, Long productId, int pricePaise) {
        this.hubId = hubId;
        this.productId = productId;
        this.pricePaise = pricePaise;
    }

    public Long getHubId() {
        return hubId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getPricePaise() {
        return pricePaise;
    }

    public boolean isActive() {
        return active;
    }
}
