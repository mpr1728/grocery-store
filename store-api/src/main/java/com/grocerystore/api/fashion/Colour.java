package com.grocerystore.api.fashion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A shade in the dye palette and what it is dyed with. */
@Entity
@Table(name = "fashion_colours")
public class Colour {

    @Id
    private String code;
    private String name;
    @Column(name = "dye_source")
    private String dyeSource;
    private String hex;
    @Column(name = "sort_order")
    private int sortOrder;
    private boolean available;

    protected Colour() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDyeSource() {
        return dyeSource;
    }

    public String getHex() {
        return hex;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isAvailable() {
        return available;
    }
}
