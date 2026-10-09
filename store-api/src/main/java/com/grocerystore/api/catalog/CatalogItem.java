package com.grocerystore.api.catalog;

/** testedBatches counts the batches on sale today that carry a pesticide residue test. */
public record CatalogItem(Long productId, String sku, String name, String category, String unitLabel, String diet,
        boolean privateLabel, String description, int pricePaise, long available, String brand, String imageUrl,
        String growingMethod, long testedBatches) {
}
