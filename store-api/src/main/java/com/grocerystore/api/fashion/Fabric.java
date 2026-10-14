package com.grocerystore.api.fashion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A cotton fabric a garment can be made in. */
@Entity
@Table(name = "fashion_fabrics")
public class Fabric {

    @Id
    private String code;
    private String name;
    private String description;
    @Column(name = "surcharge_paise")
    private int surchargePaise;
    @Column(name = "sort_order")
    private int sortOrder;
    private boolean active;

    protected Fabric() {
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

    public int getSurchargePaise() {
        return surchargePaise;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
