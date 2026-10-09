package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.catalog.CatalogItem;
import com.grocerystore.api.catalog.HubProduct;
import com.grocerystore.api.catalog.HubProductRepository;
import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductRepository;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.hub.Hub;
import com.grocerystore.api.hub.HubRepository;
import com.grocerystore.api.inventory.InsufficientStockException;
import com.grocerystore.api.inventory.StockBatch;
import com.grocerystore.api.inventory.StockBatchRepository;
import com.grocerystore.api.inventory.StockMovement;
import com.grocerystore.api.inventory.StockMovementRepository;
import com.grocerystore.api.order.OrderDtos.LineRequest;
import com.grocerystore.api.order.OrderDtos.OrderView;
import com.grocerystore.api.order.OrderDtos.PlaceOrderRequest;
import com.grocerystore.api.order.OrderService;
import com.grocerystore.api.order.OrderStatus;

@SpringBootTest
@ActiveProfiles("test")
class OrderPlacementTest {

    @Autowired
    OrderService orders;
    @Autowired
    HubRepository hubs;
    @Autowired
    ProductRepository products;
    @Autowired
    HubProductRepository hubProducts;
    @Autowired
    StockBatchRepository batches;
    @Autowired
    StockMovementRepository movements;
    @Autowired
    Clock clock;

    Hub hub;
    LocalDate today;
    int sequence;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        hub = hubs.save(new Hub("T-" + System.nanoTime(), "Test Hub", "Hyderabad", 3000));
    }

    @Test
    void picksTheBatchThatExpiresFirst() {
        Product milk = product("Milk", 3000);
        StockBatch later = batch(milk, 5, today.plusDays(3));
        StockBatch sooner = batch(milk, 5, today.plusDays(1));

        OrderView order = orders.place(request(line(milk, 7)));

        assertThat(quantityOf(sooner)).isZero();
        assertThat(quantityOf(later)).isEqualTo(3);
        List<StockMovement> recorded = movements.findByReferenceOrderById(order.orderNumber());
        assertThat(recorded).extracting(StockMovement::getBatchId).containsExactly(sooner.getId(), later.getId());
        assertThat(recorded).extracting(StockMovement::getQuantity).containsExactly(-5, -2);
        assertThat(recorded).extracting(StockMovement::getType).containsOnly(StockMovement.SALE);
    }

    @Test
    void picksDatedStockBeforeUndatedStock() {
        Product pickle = product("Pickle", 12000);
        StockBatch undated = batch(pickle, 5, null);
        StockBatch dated = batch(pickle, 5, today.plusDays(30));

        orders.place(request(line(pickle, 2)));

        assertThat(quantityOf(dated)).isEqualTo(3);
        assertThat(quantityOf(undated)).isEqualTo(5);
    }

    @Test
    void doesNotSellStockPastItsUseByDate() {
        Product curd = product("Curd", 4500);
        StockBatch expired = batch(curd, 10, today.minusDays(1));
        batch(curd, 2, today);

        assertThatThrownBy(() -> orders.place(request(line(curd, 3))))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getShortages()).hasSize(1);
                    assertThat(e.getShortages().get(0).requested()).isEqualTo(3);
                    assertThat(e.getShortages().get(0).available()).isEqualTo(2);
                });
        assertThat(quantityOf(expired)).isEqualTo(10);

        assertThat(catalogItem(curd).available()).isEqualTo(2);
    }

    @Test
    void aShortItemLeavesAllStockUntouched() {
        Product atta = product("Atta", 28500);
        Product rice = product("Rice", 42000);
        StockBatch attaBatch = batch(atta, 10, null);
        StockBatch riceBatch = batch(rice, 1, null);
        long ordersBefore = movements.count();

        assertThatThrownBy(() -> orders.place(request(line(atta, 2), line(rice, 5))))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(quantityOf(attaBatch)).isEqualTo(10);
        assertThat(quantityOf(riceBatch)).isEqualTo(1);
        assertThat(movements.count()).isEqualTo(ordersBefore);
    }

    @Test
    void chargesDeliveryBelowTheFreeThresholdOnly() {
        Product oil = product("Oil", 34000);
        batch(oil, 10, null);

        OrderView small = orders.place(request(line(oil, 1)));
        assertThat(small.itemTotalPaise()).isEqualTo(34000);
        assertThat(small.deliveryFeePaise()).isEqualTo(2900);
        assertThat(small.totalPaise()).isEqualTo(36900);

        OrderView large = orders.place(request(line(oil, 2)));
        assertThat(large.itemTotalPaise()).isEqualTo(68000);
        assertThat(large.deliveryFeePaise()).isZero();
        assertThat(large.totalPaise()).isEqualTo(68000);
    }

    @Test
    void mergesRepeatedLinesForTheSameProduct() {
        Product dal = product("Dal", 18500);
        StockBatch dalBatch = batch(dal, 10, null);

        OrderView order = orders.place(request(line(dal, 2), line(dal, 3)));

        assertThat(order.lines()).hasSize(1);
        assertThat(order.lines().get(0).quantity()).isEqualTo(5);
        assertThat(order.lines().get(0).lineTotalPaise()).isEqualTo(92500);
        assertThat(quantityOf(dalBatch)).isEqualTo(5);
    }

    @Test
    void rejectsAProductThatTheHubDoesNotSell() {
        Product elsewhere = products.save(
                new Product("SKU-" + System.nanoTime(), "Not ranged", "Staples", "1 kg", "VEG", "AMBIENT", null, false, null));

        assertThatThrownBy(() -> orders.place(request(line(elsewhere, 1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void movesAnOrderThroughEachStatusAndStopsAtDelivered() {
        Product tea = product("Tea", 9000);
        batch(tea, 5, null);
        OrderView order = orders.place(request(line(tea, 1)));
        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);

        assertThat(orders.advance(order.orderNumber()).status()).isEqualTo(OrderStatus.PICKING);
        assertThat(orders.advance(order.orderNumber()).status()).isEqualTo(OrderStatus.PACKED);
        assertThat(orders.advance(order.orderNumber()).status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(orders.advance(order.orderNumber()).status()).isEqualTo(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> orders.advance(order.orderNumber())).isInstanceOf(BadRequestException.class);

        assertThat(orders.get(order.orderNumber()).status()).isEqualTo(OrderStatus.DELIVERED);
    }

    private Product product(String name, int pricePaise) {
        Product product = products.save(new Product("SKU-" + System.nanoTime() + "-" + (sequence++), name, "Test",
                "1 unit", "VEG", "AMBIENT", null, false, null));
        hubProducts.save(new HubProduct(hub.getId(), product.getId(), pricePaise));
        return product;
    }

    private StockBatch batch(Product product, int quantity, LocalDate useBy) {
        // Later batches get a later received time so ties on use-by are predictable.
        return batches.save(new StockBatch(hub.getId(), product.getId(), "B" + (sequence++),
                Instant.now(clock).plusSeconds(sequence), useBy, quantity, "T-01"));
    }

    private int quantityOf(StockBatch batch) {
        return batches.findById(batch.getId()).orElseThrow().getQuantity();
    }

    private CatalogItem catalogItem(Product product) {
        return hubProducts.findCatalog(hub.getId(), today, null, null).stream()
                .filter(i -> i.productId().equals(product.getId()))
                .findFirst().orElseThrow();
    }

    private static LineRequest line(Product product, int quantity) {
        return new LineRequest(product.getId(), quantity);
    }

    private PlaceOrderRequest request(LineRequest... lines) {
        return new PlaceOrderRequest(hub.getId(), "9876543210", "Test Customer", "Flat 101, Test Towers", List.of(lines), null);
    }
}
