package com.grocerystore.api.inventory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductRepository;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;

@Service
public class InventoryService {

    private final StockBatchRepository batches;
    private final StockMovementRepository movements;
    private final ProductRepository products;
    private final Clock clock;

    public InventoryService(StockBatchRepository batches, StockMovementRepository movements,
            ProductRepository products, Clock clock) {
        this.batches = batches;
        this.movements = movements;
        this.products = products;
        this.clock = clock;
    }

    /** Locks the batches that can be sold today, in the order they should be picked. */
    @Transactional
    public List<StockBatch> lockSellable(Long hubId, Long productId) {
        return batches.lockSellable(hubId, productId, LocalDate.now(clock));
    }

    public static int total(List<StockBatch> sellable) {
        return sellable.stream().mapToInt(StockBatch::getQuantity).sum();
    }

    /** Takes units from locked batches in pick order and records a movement per batch touched. */
    @Transactional
    public void take(List<StockBatch> sellable, int units, String reference) {
        if (units > total(sellable)) {
            throw new IllegalStateException("Tried to take " + units + " units but only " + total(sellable) + " are sellable");
        }
        Instant now = Instant.now(clock);
        int remaining = units;
        for (StockBatch batch : sellable) {
            if (remaining == 0) {
                break;
            }
            int fromThisBatch = Math.min(remaining, batch.getQuantity());
            batch.remove(fromThisBatch);
            movements.save(new StockMovement(batch.getId(), StockMovement.SALE, -fromThisBatch, reference, now));
            remaining -= fromThisBatch;
        }
    }

    @Transactional
    public StockBatch receive(Long hubId, Long productId, int quantity, LocalDate useBy, String bin, String batchCode) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " was not found"));
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveUseBy = useBy;
        if (effectiveUseBy == null && product.getShelfLifeDays() != null) {
            effectiveUseBy = today.plusDays(product.getShelfLifeDays());
        }
        if (effectiveUseBy != null && effectiveUseBy.isBefore(today)) {
            throw new BadRequestException("Use-by date " + effectiveUseBy + " is already past");
        }
        String code = (batchCode == null || batchCode.isBlank())
                ? today.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + ThreadLocalRandom.current().nextInt(1000, 10000)
                : batchCode.trim();
        Instant now = Instant.now(clock);
        StockBatch batch = batches.save(new StockBatch(hubId, productId, code, now, effectiveUseBy, quantity, bin));
        movements.save(new StockMovement(batch.getId(), StockMovement.RECEIVE, quantity, code, now));
        return batch;
    }

    @Transactional
    public StockBatch recordTest(Long hubId, Long batchId, String lab, LocalDate date, String result, Long documentId) {
        StockBatch batch = batches.findById(batchId)
                .filter(b -> b.getHubId().equals(hubId))
                .orElseThrow(() -> new NotFoundException("Batch " + batchId + " was not found"));
        if (date.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("The test date cannot be in the future");
        }
        batch.recordTest(lab, date, result, documentId);
        return batch;
    }

    @Transactional(readOnly = true)
    public List<StockLine> stock(Long hubId) {
        return batches.findStock(hubId);
    }
}
