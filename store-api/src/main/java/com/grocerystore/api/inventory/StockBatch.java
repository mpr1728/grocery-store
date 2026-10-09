package com.grocerystore.api.inventory;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_batches")
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hub_id")
    private Long hubId;
    @Column(name = "product_id")
    private Long productId;
    @Column(name = "batch_code")
    private String batchCode;
    @Column(name = "received_at")
    private Instant receivedAt;
    @Column(name = "use_by")
    private LocalDate useBy;
    private int quantity;
    private String bin;
    @Column(name = "test_lab")
    private String testLab;
    @Column(name = "test_date")
    private LocalDate testDate;
    @Column(name = "test_result")
    private String testResult;
    @Column(name = "test_document_id")
    private Long testDocumentId;

    protected StockBatch() {
    }

    public StockBatch(Long hubId, Long productId, String batchCode, Instant receivedAt, LocalDate useBy, int quantity,
            String bin) {
        this.hubId = hubId;
        this.productId = productId;
        this.batchCode = batchCode;
        this.receivedAt = receivedAt;
        this.useBy = useBy;
        this.quantity = quantity;
        this.bin = bin;
    }

    public Long getId() {
        return id;
    }

    public Long getHubId() {
        return hubId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public LocalDate getUseBy() {
        return useBy;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getBin() {
        return bin;
    }

    void remove(int units) {
        this.quantity -= units;
    }

    /** Records the pesticide residue test for this batch. A null report keeps the one already on file. */
    public void recordTest(String lab, LocalDate date, String result, Long documentId) {
        this.testLab = lab;
        this.testDate = date;
        this.testResult = result;
        if (documentId != null) {
            this.testDocumentId = documentId;
        }
    }

    public String getTestLab() {
        return testLab;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public String getTestResult() {
        return testResult;
    }

    public Long getTestDocumentId() {
        return testDocumentId;
    }
}
