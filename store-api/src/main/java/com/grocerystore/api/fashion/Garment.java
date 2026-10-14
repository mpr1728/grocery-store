package com.grocerystore.api.fashion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A garment type the studio makes, such as a kurti. */
@Entity
@Table(name = "fashion_garments")
public class Garment {

    @Id
    private String code;
    private String name;
    private String description;
    @Column(name = "base_price_paise")
    private int basePricePaise;
    @Column(name = "making_days")
    private int makingDays;
    @Column(name = "sort_order")
    private int sortOrder;
    private boolean active;

    protected Garment() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getBasePricePaise() {
        return basePricePaise;
    }

    public int getMakingDays() {
        return makingDays;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
