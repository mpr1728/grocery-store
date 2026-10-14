package com.grocerystore.api.fashion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One choice for one part of a garment, such as a V neck on a kurti. */
@Entity
@Table(name = "fashion_options")
public class GarmentOption {

    @Id
    private Long id;
    @Column(name = "garment_code")
    private String garmentCode;
    private String part;
    private String code;
    private String label;
    @Column(name = "surcharge_paise")
    private int surchargePaise;
    @Column(name = "is_default")
    private boolean preselected;
    @Column(name = "sort_order")
    private int sortOrder;

    protected GarmentOption() {
    }

    public String getGarmentCode() {
        return garmentCode;
    }

    public String getPart() {
        return part;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getSurchargePaise() {
        return surchargePaise;
    }

    public boolean isPreselected() {
        return preselected;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
