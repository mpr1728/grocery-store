package com.grocerystore.api.fashion;

/** Where a made-to-order garment is. The customer can cancel only before the fabric is cut. */
public enum FashionOrderStatus {
    PLACED, CUTTING, STITCHING, CHECKED, DISPATCHED, DELIVERED, CANCELLED;

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean canCancel() {
        return this == PLACED;
    }

    public FashionOrderStatus next() {
        return values()[ordinal() + 1];
    }
}
