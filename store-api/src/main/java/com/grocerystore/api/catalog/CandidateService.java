package com.grocerystore.api.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.hub.HubService;

@Service
public class CandidateService {

    public record CandidatePage(long waiting, List<CatalogCandidate> candidates) {
    }

    private final CatalogCandidateRepository candidates;
    private final ProductRepository products;
    private final HubProductRepository hubProducts;
    private final HubService hubs;

    public CandidateService(CatalogCandidateRepository candidates, ProductRepository products,
            HubProductRepository hubProducts, HubService hubs) {
        this.candidates = candidates;
        this.products = products;
        this.hubProducts = hubProducts;
        this.hubs = hubs;
    }

    @Transactional(readOnly = true)
    public CandidatePage waiting() {
        return new CandidatePage(candidates.countByStatus(CatalogCandidate.NEW),
                candidates.findTop100ByStatusOrderById(CatalogCandidate.NEW));
    }

    /** Puts the candidate on sale at a hub. It starts with no stock, so it shows as out of stock until received. */
    @Transactional
    public Product approve(Long hubId, Long candidateId, int pricePaise, String diet, String category,
            String unitLabel) {
        hubs.requireActive(hubId);
        CatalogCandidate candidate = requireNew(candidateId);
        String sku = candidate.getSource() + "-" + candidate.getBarcode();
        if (products.existsBySku(sku)) {
            throw new BadRequestException("This product is already in the catalog");
        }
        Product product = new Product(sku, candidate.getName(), category.trim(), unitLabel.trim(), diet, "AMBIENT",
                null, false, null);
        product.setPackDetails(candidate.getBrand(), candidate.getBarcode(), candidate.getIngredients(),
                candidate.getImageUrl(), candidate.getSource(), candidate.getSourceUrl());
        products.save(product);
        hubProducts.save(new HubProduct(hubId, product.getId(), pricePaise));
        candidate.setStatus(CatalogCandidate.APPROVED);
        return product;
    }

    @Transactional
    public void reject(Long candidateId) {
        requireNew(candidateId).setStatus(CatalogCandidate.REJECTED);
    }

    private CatalogCandidate requireNew(Long candidateId) {
        return candidates.findById(candidateId)
                .filter(c -> CatalogCandidate.NEW.equals(c.getStatus()))
                .orElseThrow(() -> new NotFoundException("Candidate " + candidateId + " is not waiting for review"));
    }
}
