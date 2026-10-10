package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
import com.grocerystore.api.assistant.AssistantDtos.Turn;
import com.grocerystore.api.assistant.AssistantDtos.TurnItem;
import com.grocerystore.api.assistant.AssistantService;
import com.grocerystore.api.assistant.CartPlanner;
import com.grocerystore.api.assistant.CartPlanner.NutritionGoal;
import com.grocerystore.api.assistant.CartPlanner.Plan;
import com.grocerystore.api.assistant.CartPlanner.PlannedItem;
import com.grocerystore.api.cart.SavedCartService;
import com.grocerystore.api.cart.SavedCartService.Line;
import com.grocerystore.api.catalog.CatalogItem;
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
        List<Turn> lastHistory;
        String lastInstructions;

        @Override
        public Plan plan(String instructions, List<Turn> history, String message) {
            lastHistory = history;
            lastInstructions = instructions;
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
                new PlannedItem(paneer.getId(), 40, "main ingredient", null),
                new PlannedItem(paneer.getId(), 1, "repeated", null),
                new PlannedItem(gongura.getId(), 1, "out of stock", null),
                new PlannedItem(987654321L, 2, "not a real product", null)), List.of("Fresh cream"), null, null, List.of());

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("palak paneer for 4", null));

        assertThat(reply.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(paneer.getId());
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.pricePaise()).isEqualTo(9500);
        });
        assertThat(reply.notStocked()).containsExactly("Fresh cream", "Gongura (out of stock)");

        // An ingredient reported as missing that the store does have is added to the list instead.
        Product garlic = stocked("Garlic", 4000, 5);
        planner.next = new Plan("Here you go.", List.of(new PlannedItem(paneer.getId(), 1, "", null)),
                List.of("garlic", "saffron"), null, null, List.of());
        AssistantReply second = assistant.ask(hub.getId(), customer, new AskRequest("paneer curry", null));
        assertThat(second.items()).extracting(i -> i.productId()).containsExactly(paneer.getId(), garlic.getId());
        assertThat(second.notStocked()).containsExactly("saffron");
    }

    private SignUpResponse signUp(String phone) {
        return accounts.signUp(new SignUpRequest("Asha", phone, phone + "@example.com", "correct horse"));
    }

    @Test
    void assistantKeepsToAStatedBudgetAndDoesNotOverbuyFreshItems() {
        Customer customer = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        Product rice = stocked("Rice", 40000, 10);
        Product dal = stocked("Dal", 15000, 10);
        Product milk = products.save(new Product("SKU-" + System.nanoTime(), "Milk", "Test", "500 ml", "VEG", "CHILLED",
                2, false, null));
        hubProducts.save(new HubProduct(hub.getId(), milk.getId(), 3000));
        batches.save(new StockBatch(hub.getId(), milk.getId(), "B-milk", Instant.now(clock), null, 50, "T-01"));

        // 2 rice (800) + 2 dal (300) + 7 milk, which is cut to 4 (120) = 1220 rupees against a 700 rupee limit.
        planner.next = new Plan("A week of basics.", List.of(new PlannedItem(rice.getId(), 2, "", null),
                new PlannedItem(dal.getId(), 2, "", null), new PlannedItem(milk.getId(), 7, "", null)), List.of(), 700, null, List.of());

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("weekly groceries under 700", null));

        long total = reply.items().stream().mapToLong(i -> (long) i.pricePaise() * i.quantity()).sum();
        assertThat(total).isLessThanOrEqualTo(70000);
        assertThat(reply.items()).filteredOn(i -> i.productId().equals(milk.getId()))
                .allSatisfy(i -> assertThat(i.quantity()).isLessThanOrEqualTo(4));
        assertThat(reply.items()).isNotEmpty();
        assertThat(reply.notes()).anyMatch(note -> note.contains("700"));
    }

    @Test
    void aShopperWatchingTheCostGetsTheSmallerPackAndNoTreats() {
        Customer customer = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        String riceName = "Budget rice " + System.nanoTime();
        Product bigRice = products.save(new Product("SKU-big-" + System.nanoTime(), riceName, "Staples", "5 kg", "VEG", "AMBIENT", null, false, null));
        Product smallRice = products.save(new Product("SKU-small-" + System.nanoTime(), riceName, "Staples", "1 kg", "VEG", "AMBIENT", null, false, null));
        Product chips = products.save(new Product("SKU-chips-" + System.nanoTime(), "Chips", "Snacks", "50 g", "VEG", "AMBIENT", null, false, null));
        for (Object[] row : new Object[][] {{bigRice, 42000}, {smallRice, 8500}, {chips, 2000}}) {
            Product product = (Product) row[0];
            hubProducts.save(new HubProduct(hub.getId(), product.getId(), (Integer) row[1]));
            batches.save(new StockBatch(hub.getId(), product.getId(), "B" + product.getId(), Instant.now(clock), null, 10, "T-01"));
        }
        planner.next = new Plan("Basics for the week.", List.of(new PlannedItem(bigRice.getId(), 1, "", null),
                new PlannedItem(chips.getId(), 2, "", null)), List.of(), null, null, List.of());

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("budget friendly weekly groceries", null));

        assertThat(reply.items()).extracting(i -> i.productId()).containsExactly(smallRice.getId());
        assertThat(reply.reply()).isEqualTo("Basics for the week.");
        assertThat(reply.notes()).anyMatch(note -> note.contains("Tell me an amount"));
    }

    @Test
    void nutritionIsAddedUpFromPackWeightsAndComparedWithTheTarget() {
        Customer customer = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        Product dal = stocked("Protein dal " + System.nanoTime(), 18500, 10);
        dal.setNutrition(1000, 22, 1.5, 63, 15, 343, "TYPICAL");
        products.save(dal);
        Product mystery = stocked("Mystery " + System.nanoTime(), 5000, 10);
        // One 1 kg pack of dal holds 220 g of protein: about 31 g a day for one person over seven days.
        planner.next = new Plan("High protein week.", List.of(new PlannedItem(dal.getId(), 1, "", null),
                new PlannedItem(mystery.getId(), 1, "", null)), List.of(), null,
                new NutritionGoal(1, 7, 30, null, null, null, null), List.of("Lives alone", "lives alone"));

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("30 g protein a day for a week", null));

        assertThat(reply.nutrition().total().proteinG()).isEqualTo(220);
        assertThat(reply.nutrition().total().kcal()).isEqualTo(3430);
        assertThat(reply.nutrition().perPersonPerDay().proteinG()).isEqualTo(31);
        assertThat(reply.nutrition().target().proteinG()).isEqualTo(30);
        assertThat(reply.nutrition().itemsNotCounted()).isEqualTo(1);
        // A fact is kept once, and is given to the model the next time.
        assertThat(reply.remembered()).containsExactly("Lives alone");

        // Planned in grams: 700 g of dal eaten is 154 g protein, 22 g a day, from one 1 kg pack.
        planner.next = new Plan("Planned by weight.", List.of(new PlannedItem(dal.getId(), 5, "", 700)), List.of(), null,
                new NutritionGoal(1, 7, 22, null, null, null, null), List.of());
        AssistantReply byWeight = assistant.ask(hub.getId(), customer, new AskRequest("22 g protein a day", null));
        assertThat(byWeight.items()).singleElement().satisfies(item -> {
            assertThat(item.quantity()).isEqualTo(1);
            assertThat(item.gramsUsed()).isEqualTo(700);
        });
        assertThat(byWeight.nutrition().perPersonPerDay().proteinG()).isEqualTo(22);
        assertThat(planner.lastInstructions).contains("Lives alone");
    }

    @Test
    void aVegetarianNeverGetsEggs() {
        Customer customer = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        Product dal = stocked("Veg dal " + System.nanoTime(), 18500, 10);
        Product eggs = products.save(new Product("SKU-egg-" + System.nanoTime(), "Eggs " + System.nanoTime(), "Dairy and eggs", "6 pieces", "NON_VEG", "CHILLED", null, false, null));
        hubProducts.save(new HubProduct(hub.getId(), eggs.getId(), 7800));
        batches.save(new StockBatch(hub.getId(), eggs.getId(), "B-egg" + eggs.getId(), Instant.now(clock), null, 10, "T-01"));
        planner.next = new Plan("Protein for the week.", List.of(new PlannedItem(dal.getId(), 1, "", null),
                new PlannedItem(eggs.getId(), 1, "", null)), List.of(), null, null, List.of());

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("vegetarian high protein week", null));

        assertThat(reply.items()).extracting(i -> i.productId()).containsExactly(dal.getId());
        assertThat(reply.notes()).anyMatch(note -> note.contains("vegetarian"));
    }

    @Test
    void aFollowUpKeepsTheBudgetAndShowsTheModelTheEarlierList() {
        Customer customer = accounts.require(AUTH_PREFIX + signUp(newPhone()).token());
        String name = "Follow rice " + System.nanoTime();
        Product big = products.save(new Product("SKU-fb-" + System.nanoTime(), name, "Staples", "5 kg", "VEG", "AMBIENT", null, false, null));
        Product small = products.save(new Product("SKU-fs-" + System.nanoTime(), name, "Staples", "1 kg", "VEG", "AMBIENT", null, false, null));
        for (Object[] row : new Object[][] {{big, 42000}, {small, 8500}}) {
            Product product = (Product) row[0];
            hubProducts.save(new HubProduct(hub.getId(), product.getId(), (Integer) row[1]));
            batches.save(new StockBatch(hub.getId(), product.getId(), "B" + product.getId(), Instant.now(clock), null, 10, "T-01"));
        }
        // The model names both pack sizes. Only one may stay, and the earlier budget request still applies.
        planner.next = new Plan("Removed the oil.", List.of(new PlannedItem(big.getId(), 1, "", null),
                new PlannedItem(small.getId(), 1, "", null)), List.of(), null, null, List.of());
        List<Turn> history = List.of(new Turn("user", "make it budget friendly", null),
                new Turn("assistant", "Here is a list.", List.of(new TurnItem(big.getId(), 1))));

        AssistantReply reply = assistant.ask(hub.getId(), customer, new AskRequest("oil is too much", history));

        assertThat(reply.items()).extracting(i -> i.productId()).containsExactly(small.getId());
        assertThat(planner.lastHistory.get(1).text()).contains("The list the shopper was shown").contains(name).contains("420 rupees");
    }

    @Test
    void aLargeCatalogIsCutToAShortlistThatKeepsWhatMatters() {
        List<CatalogItem> catalog = new ArrayList<>();
        catalog.add(item(1L, "FV-TOM-500", "Tomato", null, "Fruit and vegetables"));
        for (long id = 100; id < 700; id++) {
            catalog.add(item(id, "OFF-" + id, "Wafer " + id, "Brand", "Snacks"));
        }
        catalog.add(item(900L, "OFF-900", "Dark chocolate", "Amul", "Biscuits and sweets"));
        catalog.add(item(901L, "OFF-901", "Plain soda", "Fizz", "Beverages"));

        List<CatalogItem> shortlist = AssistantService.shortlist(catalog,
                new AskRequest("something with chocolates for the kids",
                        List.of(new Turn("assistant", "Here you go.", List.of(new TurnItem(650L, 1))))), List.of());

        assertThat(shortlist.size()).isLessThanOrEqualTo(220);
        // The store's own food, the item already on the list, and the match for "chocolates" are all there.
        assertThat(shortlist).extracting(CatalogItem::productId).contains(1L, 650L, 900L, 901L);
    }

    private static CatalogItem item(Long id, String sku, String name, String brand, String category) {
        return new CatalogItem(id, sku, name, category, "1 pack", "VEG", false, null, 1000, 10, brand, null, null, 0, null);
    }

    @Test
    void theBudgetIsReadFromTheShoppersWords() {
        assertThat(AssistantService.statedBudget("weekly groceries for a family of 4 under 1000 rupees")).isEqualTo(1000);
        assertThat(AssistantService.statedBudget("groceries for Rs. 1,500")).isEqualTo(1500);
        assertThat(AssistantService.statedBudget("budget \u20b9800")).isEqualTo(800);
        assertThat(AssistantService.statedBudget("this is \u20b91,936")).isNull();
        assertThat(AssistantService.statedBudget("palak paneer for 4")).isNull();
        assertThat(AssistantService.statedBudget("idli for 12 people, budget friendly")).isNull();
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
