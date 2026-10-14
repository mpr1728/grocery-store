package com.grocerystore.api.fashion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.grocerystore.api.common.PhoneNumbers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class FashionDtos {

    private FashionDtos() {
    }

    public record CatalogView(List<GarmentView> garments, List<FabricView> fabrics, List<ColourView> colours,
            int shippingPaise, int freeShippingAbovePaise) {
    }

    public record GarmentView(String code, String name, String description, int basePricePaise, int makingDays,
            List<PartView> parts) {
    }

    public record PartView(String part, String label, List<OptionView> options) {
    }

    public record OptionView(String code, String label, int surchargePaise, boolean preselected) {
    }

    public record FabricView(String code, String name, String description, int surchargePaise) {
    }

    public record ColourView(String code, String name, String dyeSource, String hex, boolean available) {
    }

    /** One choice per part, plus fabric and colour, all as codes from the catalog. */
    public record DesignRequest(
            @NotBlank @Size(max = 20) String garment,
            @NotBlank @Size(max = 20) String neckline,
            @NotBlank @Size(max = 20) String sleeve,
            @NotBlank @Size(max = 20) String length,
            @NotBlank @Size(max = 20) String fit,
            @NotBlank @Size(max = 20) String fabric,
            @NotBlank @Size(max = 20) String colour) {
    }

    /** Whole centimetres. The limits only catch typing mistakes, such as inches typed as centimetres. */
    public record MeasurementsRequest(
            @NotNull @Min(60) @Max(170) Integer bustCm,
            @NotNull @Min(45) @Max(160) Integer waistCm,
            @NotNull @Min(65) @Max(180) Integer hipCm,
            @NotNull @Min(28) @Max(60) Integer shoulderCm,
            @NotNull @Min(120) @Max(200) Integer heightCm) {
    }

    public record FitView(MeasurementsRequest measurements, BodyShape bodyShape, String shapeLabel, String advice,
            Set<String> suitedFits, Instant updatedAt) {
    }

    public record PriceLine(String label, int paise) {
    }

    public record QuoteView(String garmentName, int basePricePaise, List<PriceLine> extras, int shippingPaise,
            int totalPaise, LocalDate readyBy) {
    }

    public record Choice(String part, String value) {
    }

    public record PlaceOrderRequest(
            @NotNull @Valid DesignRequest design,
            @NotNull @Valid MeasurementsRequest measurements,
            @NotBlank @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone,
            @NotBlank @Size(max = 300) String deliveryAddress,
            @Size(max = 300) String notes,
            // Plant dyes vary from batch to batch, and a garment cut to one person's measurements cannot be resold.
            @AssertTrue(message = "must be accepted: shades vary and made-to-order garments are not returnable for a change of mind") boolean acceptsTerms) {
    }

    public record OrderView(String orderNumber, FashionOrderStatus status, boolean canCancel, String customerName,
            String garmentName, List<Choice> choices, String colourHex, String designCodes,
            MeasurementsRequest measurements, String shapeLabel, int basePricePaise, int extrasPaise,
            int shippingPaise, int totalPaise, String phone, String deliveryAddress, String notes,
            Instant placedAt, Instant updatedAt, LocalDate readyBy) {
    }
}
