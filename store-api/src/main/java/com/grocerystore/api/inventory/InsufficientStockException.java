package com.grocerystore.api.inventory;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

    public record Shortage(Long productId, String productName, int requested, int available) {
    }

    private final List<Shortage> shortages;

    public InsufficientStockException(List<Shortage> shortages) {
        super("Some items do not have enough stock");
        this.shortages = shortages;
    }

    public List<Shortage> getShortages() {
        return shortages;
    }
}
