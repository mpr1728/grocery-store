package com.grocerystore.api.rewards;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.common.StoreProperties;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;

@Service
public class RewardsService {

    /** One point takes one rupee off the items at checkout. */
    public static final int POINT_VALUE_PAISE = 100;

    public static final String WELCOME = "WELCOME";
    public static final String EARNED = "EARNED";
    public static final String REDEEMED = "REDEEMED";

    private final CustomerRepository customers;
    private final PointEntryRepository entries;
    private final StoreProperties properties;

    public RewardsService(CustomerRepository customers, PointEntryRepository entries, StoreProperties properties) {
        this.customers = customers;
        this.entries = entries;
        this.properties = properties;
    }

    /** Points earned on an amount paid for items, rounded down to whole points. */
    public int pointsEarnedOn(int paidForItemsPaise) {
        return (int) ((long) paidForItemsPaise * properties.rewardEarnPercent() / 100 / POINT_VALUE_PAISE);
    }

    /** Takes points off a customer the caller has already locked. */
    public void redeem(Customer lockedCustomer, int points, String orderNumber, Instant now) {
        lockedCustomer.addPoints(-points);
        entries.save(new PointEntry(lockedCustomer.getId(), -points, REDEEMED, orderNumber, now));
    }

    @Transactional
    public void credit(Long customerId, int points, String reason, String orderNumber, Instant now) {
        Customer customer = customers.lockById(customerId).orElseThrow();
        customer.addPoints(points);
        entries.save(new PointEntry(customerId, points, reason, orderNumber, now));
    }

    @Transactional(readOnly = true)
    public List<PointEntry> recent(Long customerId) {
        return entries.findTop20ByCustomerIdOrderByIdDesc(customerId);
    }
}
