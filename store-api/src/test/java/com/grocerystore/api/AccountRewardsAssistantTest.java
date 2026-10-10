package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.account.AccountDtos.SignInRequest;
import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountDtos.SignUpResponse;
import com.grocerystore.api.account.AccountDtos.SignedIn;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.assistant.AssistantDtos.AskRequest;
import com.grocerystore.api.assistant.AssistantDtos.AssistantReply;
import com.grocerystore.api.assistant.AssistantService;
import com.grocerystore.api.assistant.CartPlanner;
import com.grocerystore.api.assistant.CartPlanner.Plan;
import com.grocerystore.api.assistant.CartPlanner.PlannedItem;
import com.grocerystore.api.cart.SavedCartService;
import com.grocerystore.api.cart.SavedCartService.Line;
import com.grocerystore.api.catalog.HubProduct;
import com.grocerystore.api.catalog.HubProductRepository;
import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductRepository;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.UnauthorizedException;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.hub.Hub;
import com.grocerystore.api.hub.HubRepository;
import com.grocerystore.api.inventory.StockBatch;
import com.grocerystore.api.inventory.StockBatchRepository;
import com.grocerystore.api.order.OrderDtos.LineRequest;
import com.grocerystore.api.order.OrderDtos.OrderView;
import com.grocerystore.api.order.OrderDtos.PlaceOrderRequest;
import com.grocerystore.api.order.OrderService;

@SpringBootTest
@ActiveProfiles("test")
class AccountRewardsAssistantTest {

    /** Stands in for the model. Each test sets the plan it should return. */
    static class FakePlanner implements CartPlanner {
        Plan next;

        @Override
        public Plan plan(String instructions, List<com.grocerystore.api.assistant.AssistantDtos.Turn> history,
                String message) {
            return next;
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        FakePlanner fakePlanner() {
            return new FakePlanner();
        }
    }

    private static final String AUTH_PREFIX = "Bearer ";
    private static long phoneSequence = 9000000000L;

    @Autowired
    AccountService accounts;
    @Autowired
    OrderService orders;
    @Autowired
    AssistantService assistant;
    @Autowired
    SavedCartService carts;
    @Autowired
    FakePlanner planner;
    @Autowired
    HubRepository hubs;
    @Autowired
    ProductRepository products;
    @Autowired
    HubProductRepository hubProducts;
    @Autowired
    StockBatchRepository batches;
    @Autowired
    Clock clock;

    Hub hub;
    int sequence;

    @BeforeEach
    void setUp() {
        hub = hubs.save(new Hub("A-" + System.nanoTime(), "Test Hub", "Hyderabad", 3000));
    }

    @Test
    void signUpGivesWelcomePointsAndASessionThatSignsIn() {
        String phone = newPhone();
        SignUpResponse signedUp = signUp(phone);

        assertThat(signedUp.account().pointsBalance()).isEqualTo(50);
        assertThat(signedUp.account().recentPoints()).singleElement()
                .satisfies(entry -> assertThat(entry.reason()).isEqualTo("WELCOME"));
        assertThat(accounts.require(AUTH_PREFIX + signedUp.token()).getPhone()).isEqualTo("+91" + phone);

        SignedIn again = accounts.signIn(new SignInRequest(phone, "correct horse"));
        assertThat(again.token()).isNotEqualTo(signedUp.token());
        // The email address signs in too, whatever its letter case.
        assertThat(accounts.signIn(new SignInRequest(phone + "@EXAMPLE.com", "correct horse")).account().phone())
                .isEqualTo("+91" + phone);

        accounts.signOut(AUTH_PREFIX + signedUp.token());
        assertThatThrownBy(() -> accounts.require(AUTH_PREFIX + signedUp.token()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsAWrongPasswordASecondSignUpAndAMissingSession() {
        String phone = newPhone();
        signUp(phone);

        assertThatThrownBy(() -> accounts.signIn(new SignInRequest(phone, "wrong horse")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accounts.signUp(new SignUpRequest("Someone Else", phone, "else" + phone + "@example.com", "another one")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accounts.require(null)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> accounts.require(AUTH_PREFIX + "made-up-token"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void memberSpendsPointsAtCheckoutAndEarnsNewOnesOnDelivery() {
        String phone = newPhone();
        SignUpResponse member = signUp(phone);
        Customer customer = accounts.require(AUTH_PREFIX + member.token());
        Product rice = stocked("Rice", 30000, 10);

        OrderView order = orders.place(request(phone, 50, new LineRequest(rice.getId(), 2)), customer.getId());

        assertThat(order.itemTotalPaise()).isEqualTo(60000);
        assertThat(order.pointsDiscountPaise()).isEqualTo(5000);
        assertThat(order.deliveryFeePaise()).isZero();
        assertThat(order.totalPaise()).isEqualTo(55000);
        // 2 percent of the 550 rupees paid for items.
        assertThat(order.pointsEarned()).isEqualTo(11);
        assertThat(balance(member)).isZero();

        for (int i = 0; i < 3; i++) {
            orders.advance(order.orderNumber());
        }
        assertThat(balance(member)).isZero();
        orders.advance(order.orderNumber());
        assertThat(balance(member)).isEqualTo(11);
    }

    @Test
    void pointsCannotBeOverspentOrUsedByAGuest() {
        String phone = newPhone();
        SignUpResponse member = signUp(phone);
        Customer customer = accounts.require(AUTH_PREFIX + member.token());
        Product rice = stocked("Rice", 30000, 10);
        Product chilli = stocked("Green chilli", 2000, 10);

        assertThatThrownBy(() -> orders.place(request(phone, 51, new LineRequest(rice.getId(), 1)), customer.getId()))
                .isInstanceOf(BadRequestException.class);
        // 50 points are worth more than this 20 rupee basket.
        assertThatThrownBy(() -> orders.place(request(phone, 50, new LineRequest(chilli.getId(), 1)), customer.getId()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> orders.place(request(newPhone(), 10, new LineRequest(rice.getId(), 1))))
                .isInstanceOf(BadRequestException.class);
        assertThat(balance(member)).isEqualTo(50);

        OrderView guestOrder = orders.place(request(newPhone(), null, new LineRequest(rice.getId(), 1)));
        assertThat(guestOrder.pointsEarned()).isZero();
    }

    @Test
    void aSavedCartIsReplacedAsAWholeAndBelongsToOneCustomer() {
        Customer asha = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        Customer other = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        Product rice = stocked("Rice", 30000, 10);
        Product dal = stocked("Dal", 15000, 10);

        carts.replace(asha.getId(), List.of(new Line(rice.getId(), 2), new Line(dal.getId(), 1)));
        carts.replace(asha.getId(), List.of(new Line(dal.getId(), 3), new Line(987654321L, 1)));

        assertThat(carts.get(asha.getId())).containsExactly(new Line(dal.getId(), 3));
        assertThat(carts.get(other.getId())).isEmpty();

        carts.clear(asha.getId());
        assertThat(carts.get(asha.getId())).isEmpty();
    }

    @Test
    void assistantKeepsOnlyRealInStockProductsAndSaneQuantities() {
        Customer customer = accounts.require(
                AUTH_PREFIX + signUp(newPhone()).token());
        Product paneer = stocked("Paneer", 9500, 3);
        Product gongura = stocked("Gongura", 2500, 0);
        planner.next = new Plan("Here is your palak paneer.", List.of(
                new PlannedItem(paneer.getId(), 40, "main ingredient"),
                new PlannedItem(paneer.getId(), 1, "repeated"),
                new PlannedItem(gongura.getId(), 1, "out of stock"),
                new PlannedItem(987654321L, 2, "not a real product")), List.of("Fresh cream"));

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("palak paneer for 4", null));

        assertThat(reply.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(paneer.getId());
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.pricePaise()).isEqualTo(9500);
        });
        assertThat(reply.notStocked()).containsExactly("Fresh cream", "Gongura (out of stock)");
    }

    private SignUpResponse signUp(String phone) {
        return accounts.signUp(new SignUpRequest("Asha", phone, phone + "@example.com", "correct horse"));
    }

    private int balance(SignUpResponse member) {
        return accounts.view(accounts.require(AUTH_PREFIX + member.token())).pointsBalance();
    }

    private Product stocked(String name, int pricePaise, int quantity) {
        Product product = products.save(new Product("SKU-" + System.nanoTime() + "-" + (sequence++), name, "Test",
                "1 unit", "VEG", "AMBIENT", null, false, null));
        hubProducts.save(new HubProduct(hub.getId(), product.getId(), pricePaise));
        if (quantity > 0) {
            batches.save(new StockBatch(hub.getId(), product.getId(), "B" + (sequence++), Instant.now(clock), null,
                    quantity, "T-01"));
        }
        return product;
    }

    private PlaceOrderRequest request(String phone, Integer points, LineRequest... lines) {
        return new PlaceOrderRequest(hub.getId(), phone, "Asha", "Flat 101, Test Towers", List.of(lines), points);
    }

    private static String newPhone() {
        return String.valueOf(phoneSequence++);
    }
}
