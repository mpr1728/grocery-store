package com.grocerystore.api.inventory;

import java.time.Instant;
import java.time.LocalDate;

public record StockLine(Long batchId, Long productId, String productName, String unitLabel, String batchCode,
        int quantity, LocalDate useBy, String bin, Instant receivedAt, String testLab, LocalDate testDate,
        String testResult, Long testDocumentId) {
}
