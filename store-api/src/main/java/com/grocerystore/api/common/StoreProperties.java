package com.grocerystore.api.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "store")
public record StoreProperties(String timezone, int deliveryFeePaise, int freeDeliveryAbovePaise, boolean seedSampleData,
        int rewardEarnPercent, int rewardWelcomePoints, String assistantFunction, int assistantDailyLimit, String brandName, String brandTagline, String mailFrom,
        String originSecret) {
}
