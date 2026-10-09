package com.grocerystore.api.catalog;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.hub.HubService;
import com.grocerystore.api.inventory.StockBatch;
import com.grocerystore.api.inventory.StockBatchRepository;

@Service
public class ProductService {

    /**
     * CERTIFIED_ORGANIC: a certificate exists (NPOP or PGS-India). GROWER_DECLARED: the grower states that no
     * synthetic pesticides were used. Neither says anything about residue, which is tested per batch.
     */
    public static final Set<String> GROWING_METHODS = Set.of("CERTIFIED_ORGANIC", "GROWER_DECLARED");

    public record ProductDetail(Long productId, String name, String brand, String category, String unitLabel,
            String diet, boolean privateLabel, String description, String ingredients, String imageUrl,
            String source, String sourceUrl, int pricePaise, int available, String growingMethod, String certifier,
            String certificateNumber, Long certificateDocumentId, List<BatchEvidence> batches) {
    }

    /** One batch on sale today and its residue test, if it has one. */
    public record BatchEvidence(String batchCode, Instant receivedAt, LocalDate useBy, int quantity, String testLab,
            LocalDate testDate, String testResult, Long testDocumentId) {
    }

    private final HubService hubs;
    private final HubProductRepository hubProducts;
    private final ProductRepository products;
    private final StockBatchRepository batches;
    private final Clock clock;

    public ProductService(HubService hubs, HubProductRepository hubProducts, ProductRepository products,
            StockBatchRepository batches, Clock clock) {
        this.hubs = hubs;
        this.hubProducts = hubProducts;
        this.products = products;
        this.batches = batches;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProductDetail detail(Long hubId, Long productId) {
        hubs.requireActive(hubId);
        HubProduct hubProduct = hubProducts.findByHubIdAndProductIdAndActiveTrue(hubId, productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " is not sold at this hub"));
        Product p = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " was not found"));
        List<StockBatch> sellable = batches.findSellable(hubId, productId, LocalDate.now(clock));
        List<BatchEvidence> evidence = sellable.stream()
                .map(b -> new BatchEvidence(b.getBatchCode(), b.getReceivedAt(), b.getUseBy(), b.getQuantity(),
                        b.getTestLab(), b.getTestDate(), b.getTestResult(), b.getTestDocumentId()))
                .toList();
        return new ProductDetail(p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getUnitLabel(),
                p.getDiet(), p.isPrivateLabel(), p.getDescription(), p.getIngredients(), p.getImageUrl(),
                p.getSource(), p.getSourceUrl(), hubProduct.getPricePaise(),
                sellable.stream().mapToInt(StockBatch::getQuantity).sum(), p.getGrowingMethod(), p.getCertifier(),
                p.getCertificateNumber(), p.getCertificateDocumentId(), evidence);
    }

    /** An empty method clears the claim. certificateDocumentId may be null to keep the certificate on file. */
    @Transactional
    public void setGrowing(Long productId, String growingMethod, String certifier, String certificateNumber,
            Long certificateDocumentId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " was not found"));
        String method = blankToNull(growingMethod);
        if (method != null && !GROWING_METHODS.contains(method)) {
            throw new BadRequestException("The growing method must be one of " + GROWING_METHODS);
        }
        String by = blankToNull(certifier);
        String number = blankToNull(certificateNumber);
        if ("CERTIFIED_ORGANIC".equals(method) && (by == null || number == null)) {
            throw new BadRequestException("Certified organic needs the certifier's name and the certificate number");
        }
        if ((by != null && by.length() > 100) || (number != null && number.length() > 60)) {
            throw new BadRequestException("The certifier or certificate number is too long");
        }
        product.setGrowing(method, by, number, certificateDocumentId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
