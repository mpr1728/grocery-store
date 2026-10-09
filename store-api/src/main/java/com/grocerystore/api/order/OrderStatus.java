package com.grocerystore.api.order;

public enum OrderStatus {
    PLACED, PICKING, PACKED, OUT_FOR_DELIVERY, DELIVERED;

    public boolean isFinal() {
        return this == DELIVERED;
    }

    public OrderStatus next() {
        return values()[ordinal() + 1];
    }
}
