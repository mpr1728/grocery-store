package com.grocerystore.api.inventory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.DocumentService;
import com.grocerystore.api.hub.HubService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/hubs/{hubId}/stock")
public class StockController {

    public record StockView(Long batchId, Long productId, String productName, String unitLabel, String batchCode,
            int quantity, LocalDate useBy, Long daysLeft, String bin, String testLab, LocalDate testDate,
            String testResult, Long testDocumentId) {
    }

    public record ReceiptView(Long batchId, Long productId, String batchCode, int quantity, LocalDate useBy, String bin) {
    }

    public record ReceiptRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity, LocalDate useBy,
            @Size(max = 20) String bin, @Size(max = 40) String batchCode) {
    }

    /** NONE_DETECTED: no residue found. WITHIN_LIMITS: residue found but under the legal limit. */
    private static final Set<String> TEST_RESULTS = Set.of("NONE_DETECTED", "WITHIN_LIMITS");

    private final HubService hubs;
    private final InventoryService inventory;
    private final DocumentService documents;
    private final Clock clock;

    public StockController(HubService hubs, InventoryService inventory, DocumentService documents, Clock clock) {
        this.hubs = hubs;
        this.inventory = inventory;
        this.documents = documents;
        this.clock = clock;
    }

    @GetMapping
    public List<StockView> stock(@PathVariable Long hubId) {
        hubs.requireActive(hubId);
        LocalDate today = LocalDate.now(clock);
        return inventory.stock(hubId).stream()
                .map(s -> new StockView(s.batchId(), s.productId(), s.productName(), s.unitLabel(), s.batchCode(),
                        s.quantity(), s.useBy(),
                        s.useBy() == null ? null : ChronoUnit.DAYS.between(today, s.useBy()), s.bin(), s.testLab(),
                        s.testDate(), s.testResult(), s.testDocumentId()))
                .toList();
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptView receive(@PathVariable Long hubId, @Valid @RequestBody ReceiptRequest request) {
        hubs.requireActive(hubId);
        StockBatch batch = inventory.receive(hubId, request.productId(), request.quantity(), request.useBy(),
                request.bin(), request.batchCode());
        return new ReceiptView(batch.getId(), batch.getProductId(), batch.getBatchCode(), batch.getQuantity(),
                batch.getUseBy(), batch.getBin());
    }

    /** Records a batch's pesticide residue test. The lab report is optional but is what shoppers can open. */
    @PostMapping(value = "/batches/{batchId}/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void recordTest(@PathVariable Long hubId, @PathVariable Long batchId, @RequestParam String lab,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate testDate,
            @RequestParam String result, @RequestParam(required = false) MultipartFile report) {
        hubs.requireActive(hubId);
        if (lab.isBlank() || lab.length() > 100) {
            throw new BadRequestException("Give the lab's name, up to 100 characters");
        }
        if (!TEST_RESULTS.contains(result)) {
            throw new BadRequestException("The result must be one of " + TEST_RESULTS);
        }
        inventory.recordTest(hubId, batchId, lab.trim(), testDate, result, documents.store(report));
    }
}
