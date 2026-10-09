package com.grocerystore.api.catalog;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A product found by a catalog import, waiting for a person to approve it and set its price. */
@Entity
@Table(name = "catalog_candidates")
public class CatalogCandidate {

    public static final String NEW = "NEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String barcode;
    private String name;
    private String brand;
    @Column(name = "unit_label")
    private String unitLabel;
    private String category;
    private String diet;
    private String ingredients;
    private String labels;
    @Column(name = "image_url")
    private String imageUrl;
    private String source;
    @Column(name = "source_url")
    private String sourceUrl;
    private String status;
    @Column(name = "created_at")
    private Instant createdAt;

    protected CatalogCandidate() {
    }

    public CatalogCandidate(String barcode, String name, String brand, String unitLabel, String category, String diet,
            String ingredients, String labels, String imageUrl, String source, String sourceUrl, Instant createdAt) {
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.unitLabel = unitLabel;
        this.category = category;
        this.diet = diet;
        this.ingredients = ingredients;
        this.labels = labels;
        this.imageUrl = imageUrl;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.status = NEW;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public String getCategory() {
        return category;
    }

    public String getDiet() {
        return diet;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getLabels() {
        return labels;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
