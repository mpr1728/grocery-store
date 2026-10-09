package com.grocerystore.api.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String name;
    private String category;
    @Column(name = "unit_label")
    private String unitLabel;
    private String diet;
    @Column(name = "storage_zone")
    private String storageZone;
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;
    @Column(name = "private_label")
    private boolean privateLabel;
    private String description;
    private String brand;
    private String barcode;
    private String ingredients;
    @Column(name = "image_url")
    private String imageUrl;
    private String source;
    @Column(name = "source_url")
    private String sourceUrl;
    @Column(name = "growing_method")
    private String growingMethod;
    private String certifier;
    @Column(name = "certificate_number")
    private String certificateNumber;
    @Column(name = "certificate_document_id")
    private Long certificateDocumentId;

    protected Product() {
    }

    public Product(String sku, String name, String category, String unitLabel, String diet, String storageZone,
            Integer shelfLifeDays, boolean privateLabel, String description) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unitLabel = unitLabel;
        this.diet = diet;
        this.storageZone = storageZone;
        this.shelfLifeDays = shelfLifeDays;
        this.privateLabel = privateLabel;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public String getDiet() {
        return diet;
    }

    public String getStorageZone() {
        return storageZone;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public boolean isPrivateLabel() {
        return privateLabel;
    }

    public String getDescription() {
        return description;
    }

    /** Details that come with a packaged product, usually from a catalog import. */
    public void setPackDetails(String brand, String barcode, String ingredients, String imageUrl, String source,
            String sourceUrl) {
        this.brand = brand;
        this.barcode = barcode;
        this.ingredients = ingredients;
        this.imageUrl = imageUrl;
        this.source = source;
        this.sourceUrl = sourceUrl;
    }

    /** How it was grown and who certified it. A null certificate keeps the one already on file. */
    public void setGrowing(String growingMethod, String certifier, String certificateNumber, Long certificateDocumentId) {
        this.growingMethod = growingMethod;
        this.certifier = certifier;
        this.certificateNumber = certificateNumber;
        if (certificateDocumentId != null) {
            this.certificateDocumentId = certificateDocumentId;
        }
    }

    public String getBrand() {
        return brand;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getIngredients() {
        return ingredients;
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

    public String getGrowingMethod() {
        return growingMethod;
    }

    public String getCertifier() {
        return certifier;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public Long getCertificateDocumentId() {
        return certificateDocumentId;
    }
}
