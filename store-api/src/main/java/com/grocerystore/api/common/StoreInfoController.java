package com.grocerystore.api.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.rewards.RewardsService;

/** Lets the storefront show the all-in price before checkout without copying the fee rules. */
@RestController
@RequestMapping("/api/store")
public class StoreInfoController {

    public record StoreInfo(int deliveryFeePaise, int freeDeliveryAbovePaise, int rewardEarnPercent,
            int pointValuePaise, boolean assistantEnabled) {
    }

    private final StoreProperties properties;

    public StoreInfoController(StoreProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public StoreInfo info() {
        String assistant = properties.assistantFunction();
        return new StoreInfo(properties.deliveryFeePaise(), properties.freeDeliveryAbovePaise(),
                properties.rewardEarnPercent(), RewardsService.POINT_VALUE_PAISE, assistant != null && !assistant.isBlank());
    }
}
