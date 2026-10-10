package com.grocerystore.api.catalog;

/**
 * testedBatches counts the batches on sale today that carry a pesticide residue test.
 * shelfLifeDays is how long the product keeps once received, or null when it keeps for months.
 */
public record CatalogItem(Long productId, String sku, String name, String category, String unitLabel, String diet,
        boolean privateLabel, String description, int pricePaise, long available, String brand, String imageUrl,
        String growingMethod, long testedBatches, Integer shelfLifeDays) {
}
