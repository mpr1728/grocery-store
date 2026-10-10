package com.grocerystore.api.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.catalog.HubProduct;
import com.grocerystore.api.catalog.HubProductRepository;
import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductRepository;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.common.PhoneNumbers;
import com.grocerystore.api.common.StoreProperties;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;
import com.grocerystore.api.hub.HubService;
import com.grocerystore.api.inventory.InsufficientStockException;
import com.grocerystore.api.inventory.InsufficientStockException.Shortage;
import com.grocerystore.api.inventory.InventoryService;
import com.grocerystore.api.inventory.StockBatch;
import com.grocerystore.api.order.OrderDtos.LineRequest;
import com.grocerystore.api.order.OrderDtos.LineView;
import com.grocerystore.api.order.OrderDtos.OrderView;
import com.grocerystore.api.order.OrderDtos.PlaceOrderRequest;
import com.grocerystore.api.rewards.RewardsService;

@Service
public class OrderService {

    private static final String ORDER_NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final HubService hubs;
    private final HubProductRepository hubProducts;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final InventoryService inventory;
    private final RewardsService rewards;
    private final StoreProperties properties;
    private final Clock clock;

    public OrderService(HubService hubs, HubProductRepository hubProducts, ProductRepository products,
            CustomerRepository customers, OrderRepository orders, InventoryService inventory,
            RewardsService rewards, StoreProperties properties, Clock clock) {
        this.hubs = hubs;
        this.hubProducts = hubProducts;
        this.products = products;
        this.customers = customers;
        this.orders = orders;
        this.inventory = inventory;
        this.rewards = rewards;
        this.properties = properties;
        this.clock = clock;
    }

    /** Places an order as a guest. */
    @Transactional
    public OrderView place(PlaceOrderRequest request) {
        return place(request, null);
    }

    /** Places an order. memberId is the signed-in customer, or null for a guest. Only members use and earn points. */
    @Transactional
    public OrderView place(PlaceOrderRequest request, Long memberId) {
        hubs.requireActive(request.hubId());

        // The same product may arrive on two lines. Sorting by product id keeps lock order stable across orders.
        Map<Long, Integer> quantities = new TreeMap<>();
        for (LineRequest line : request.lines()) {
            quantities.merge(line.productId(), line.quantity(), Math::addExact);
        }

        record Priced(Product product, HubProduct hubProduct, int quantity, List<StockBatch> sellable) {
        }

        List<Priced> priced = new ArrayList<>();
        List<Shortage> shortages = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            HubProduct hubProduct = hubProducts.findByHubIdAndProductIdAndActiveTrue(request.hubId(), productId)
                    .orElseThrow(() -> new BadRequestException("Product " + productId + " is not sold at this hub"));
            Product product = products.findById(productId)
                    .orElseThrow(() -> new BadRequestException("Product " + productId + " is not sold at this hub"));
            List<StockBatch> sellable = inventory.lockSellable(request.hubId(), productId);
            int available = InventoryService.total(sellable);
            if (quantity > available) {
                shortages.add(new Shortage(productId, product.getName(), quantity, available));
            }
            priced.add(new Priced(product, hubProduct, quantity, sellable));
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }

        String orderNumber = newOrderNumber();
        List<OrderLine> lines = new ArrayList<>();
        for (Priced p : priced) {
            inventory.take(p.sellable(), p.quantity(), orderNumber);
            lines.add(new OrderLine(p.product().getId(), p.product().getName(), p.product().getUnitLabel(),
                    p.hubProduct().getPricePaise(), p.quantity()));
        }

        Instant now = Instant.now(clock);
        String phone = PhoneNumbers.normalize(request.phone());
        Customer customer;
        if (memberId != null) {
            customer = customers.lockById(memberId).orElseThrow();
        } else {
            customer = customers.findByPhone(phone)
                    .orElseGet(() -> customers.save(new Customer(phone, null, now)));
            // A guest typing an account holder's number must not be able to rename that account.
            if (!customer.hasAccount() && request.customerName() != null && !request.customerName().isBlank()) {
                customer.setName(request.customerName().trim());
            }
        }

        int itemTotal = lines.stream().mapToInt(OrderLine::getLineTotalPaise).reduce(0, Math::addExact);
        int deliveryFee = itemTotal >= properties.freeDeliveryAbovePaise() ? 0 : properties.deliveryFeePaise();

        int redeem = request.pointsToRedeem() == null ? 0 : request.pointsToRedeem();
        if (redeem > 0) {
            if (memberId == null) {
                throw new BadRequestException("Sign in to use reward points");
            }
            if (redeem > customer.getPointsBalance()) {
                throw new BadRequestException("You have " + customer.getPointsBalance() + " points to use");
            }
            if (redeem > itemTotal / RewardsService.POINT_VALUE_PAISE) {
                throw new BadRequestException("Points can cover the items only, not more than their total");
            }
        }
        int discount = redeem * RewardsService.POINT_VALUE_PAISE;
        // Points are earned on what was paid for items, and credited when the order is delivered.
        int earned = memberId == null ? 0 : rewards.pointsEarnedOn(itemTotal - discount);

        CustomerOrder order = orders.save(new CustomerOrder(orderNumber, request.hubId(), customer.getId(),
                phone, request.deliveryAddress().trim(), lines, deliveryFee, redeem, discount, earned, now));
        if (redeem > 0) {
            rewards.redeem(customer, redeem, orderNumber, now);
        }
        return view(order, customer);
    }

    @Transactional(readOnly = true)
    public OrderView get(String orderNumber) {
        CustomerOrder order = require(orderNumber);
        return view(order, customers.findById(order.getCustomerId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<OrderView> recentForHub(Long hubId) {
        hubs.requireActive(hubId);
        return orders.findTop100ByHubIdOrderByCreatedAtDescIdDesc(hubId).stream()
                .map(o -> view(o, customers.findById(o.getCustomerId()).orElseThrow()))
                .toList();
    }

    /** What a customer bought in their last few orders, most often bought first, for the shopping assistant. */
    @Transactional(readOnly = true)
    public List<String> recentlyBought(Long customerId) {
        Map<String, Integer> counts = new TreeMap<>();
        for (CustomerOrder order : orders.findTop5ByCustomerIdOrderByIdDesc(customerId)) {
            for (OrderLine line : order.getLines()) {
                counts.merge(line.getProductName(), line.getQuantity(), Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(15)
                .map(e -> e.getKey() + " x" + e.getValue())
                .toList();
    }

    /** Moves the order one step forward: placed, picking, packed, out for delivery, delivered. */
    @Transactional
    public OrderView advance(String orderNumber) {
        CustomerOrder order = require(orderNumber);
        if (order.getStatus().isFinal()) {
            throw new BadRequestException("Order " + orderNumber + " is already delivered");
        }
        order.setStatus(order.getStatus().next());
        if (order.getStatus() == OrderStatus.DELIVERED && order.getPointsEarned() > 0) {
            rewards.credit(order.getCustomerId(), order.getPointsEarned(), RewardsService.EARNED, orderNumber,
                    Instant.now(clock));
        }
        return view(order, customers.findById(order.getCustomerId()).orElseThrow());
    }

    private CustomerOrder require(String orderNumber) {
        return orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order " + orderNumber + " was not found"));
    }

    private String newOrderNumber() {
        String day = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyMMdd"));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (true) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                suffix.append(ORDER_NUMBER_ALPHABET.charAt(random.nextInt(ORDER_NUMBER_ALPHABET.length())));
            }
            String candidate = day + "-" + suffix;
            if (!orders.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
    }

    private static OrderView view(CustomerOrder order, Customer customer) {
        List<LineView> lines = order.getLines().stream()
                .map(l -> new LineView(l.getProductId(), l.getProductName(), l.getUnitLabel(), l.getUnitPricePaise(),
                        l.getQuantity(), l.getLineTotalPaise()))
                .toList();
        return new OrderView(order.getOrderNumber(), order.getStatus(), order.getHubId(), customer.getName(),
                order.getContactPhone() != null ? order.getContactPhone() : customer.getPhone(), order.getDeliveryAddress(), lines, order.getItemTotalPaise(),
                order.getDeliveryFeePaise(), order.getPointsRedeemed(), order.getPointsDiscountPaise(),
                order.getPointsEarned(), order.getTotalPaise(), order.getCreatedAt());
    }
}
