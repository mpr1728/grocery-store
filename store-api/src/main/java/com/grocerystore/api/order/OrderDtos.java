package com.grocerystore.api.order;

import java.time.Instant;
import java.util.List;

import com.grocerystore.api.common.PhoneNumbers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotNull Long hubId,
            @NotBlank @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone,
            @Size(max = 100) String customerName,
            @NotBlank @Size(max = 300) String deliveryAddress,
            @NotEmpty @Size(max = 100) @Valid List<LineRequest> lines,
            @Min(0) Integer pointsToRedeem) {
    }

    public record LineRequest(@NotNull Long productId, @NotNull @Min(1) @Max(50) Integer quantity) {
    }

    public record OrderView(String orderNumber, OrderStatus status, Long hubId, String customerName, String phone,
            String deliveryAddress, List<LineView> lines, int itemTotalPaise, int deliveryFeePaise, int pointsRedeemed,
            int pointsDiscountPaise, int pointsEarned, int totalPaise, Instant createdAt) {
    }

    public record LineView(Long productId, String productName, String unitLabel, int unitPricePaise, int quantity,
            int lineTotalPaise) {
    }
}
