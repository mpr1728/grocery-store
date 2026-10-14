package com.grocerystore.api.fashion;

import java.time.LocalDate;

/** A checked design with its price. extrasPaise is every option and fabric surcharge added together. */
record FashionQuote(Garment garment, GarmentOption neckline, GarmentOption sleeve, GarmentOption length,
        GarmentOption fit, Fabric fabric, Colour colour, int extrasPaise, int shippingPaise, int totalPaise,
        LocalDate readyBy) {
}
