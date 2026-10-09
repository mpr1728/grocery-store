package com.grocerystore.api.dev;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.catalog.HubProduct;
import com.grocerystore.api.catalog.HubProductRepository;
import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductRepository;
import com.grocerystore.api.hub.Hub;
import com.grocerystore.api.hub.HubRepository;
import com.grocerystore.api.inventory.InventoryService;

/** Loads one sample hub with a small range so the app is usable right after start. Local use only. */
@Component
@ConditionalOnProperty(name = "store.seed-sample-data", havingValue = "true")
public class SampleDataSeeder implements CommandLineRunner {

    private final HubRepository hubs;
    private final ProductRepository products;
    private final HubProductRepository hubProducts;
    private final InventoryService inventory;
    private final Clock clock;

    public SampleDataSeeder(HubRepository hubs, ProductRepository products, HubProductRepository hubProducts,
            InventoryService inventory, Clock clock) {
        this.hubs = hubs;
        this.products = products;
        this.hubProducts = hubProducts;
        this.inventory = inventory;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (hubs.count() > 0) {
            return;
        }
        Hub hub = hubs.save(new Hub("HYD-GCB", "Gachibowli Hub", "Hyderabad", 3000));
        LocalDate today = LocalDate.now(clock);

        // sku, name, category, unit, diet, zone, shelf life days, private label, description, rupees, stock, bin
        add(hub, today, "FV-TOM-500", "Tomato, country", "Fruit and vegetables", "500 g", "VEG", "PRODUCE", 4, false,
                "Graded, from farms near Shamshabad.", 32, 60, "P-01");
        add(hub, today, "FV-ONI-1K", "Onion", "Fruit and vegetables", "1 kg", "VEG", "PRODUCE", 14, false,
                "Medium size, dry skin.", 45, 80, "P-02");
        add(hub, today, "FV-BAN-6", "Banana, Robusta", "Fruit and vegetables", "6 pieces", "VEG", "PRODUCE", 4, false,
                "Ripe and ready to eat.", 48, 40, "P-03");
        add(hub, today, "FV-PAL-250", "Palak (spinach)", "Fruit and vegetables", "250 g", "VEG", "PRODUCE", 2, false,
                "Cleaned and trimmed.", 28, 25, "P-04");
        add(hub, today, "FV-GON-200", "Gongura", "Fruit and vegetables", "200 g", "VEG", "PRODUCE", 2, false,
                "Red stem sorrel leaves.", 25, 0, "P-05");
        add(hub, today, "DE-MLK-500", "Cow milk, toned", "Dairy and eggs", "500 ml", "VEG", "CHILLED", 2, false,
                "From a local dairy. Tested for adulteration.", 30, 120, "C-01");
        add(hub, today, "DE-CRD-400", "Curd", "Dairy and eggs", "400 g", "VEG", "CHILLED", 7, false,
                "Set curd, no preservatives.", 45, 50, "C-02");
        add(hub, today, "DE-PNR-200", "Paneer", "Dairy and eggs", "200 g", "VEG", "CHILLED", 7, false,
                "Fresh, soft paneer.", 95, 30, "C-03");
        add(hub, today, "DE-EGG-6", "Eggs, free range", "Dairy and eggs", "6 pieces", "NON_VEG", "CHILLED", 14, false,
                "Brown eggs from free range hens.", 78, 45, "C-04");
        add(hub, today, "ST-ATT-5K", "Whole wheat atta", "Staples", "5 kg", "VEG", "AMBIENT", null, true,
                "Stone ground chakki atta. House brand.", 285, 35, "A-01");
        add(hub, today, "ST-RIC-5K", "Sona Masoori rice", "Staples", "5 kg", "VEG", "AMBIENT", null, true,
                "Aged 12 months. House brand.", 420, 30, "A-02");
        add(hub, today, "ST-TDL-1K", "Toor dal", "Staples", "1 kg", "VEG", "AMBIENT", null, true,
                "Unpolished. House brand.", 185, 40, "A-03");
        add(hub, today, "ST-GNO-1L", "Groundnut oil, cold pressed", "Staples", "1 litre", "VEG", "AMBIENT", null, true,
                "Wood pressed. House brand.", 340, 24, "A-04");
        add(hub, today, "ST-TUR-200", "Turmeric powder", "Staples", "200 g", "VEG", "AMBIENT", null, true,
                "Tested for added colour. House brand.", 72, 50, "A-05");
        add(hub, today, "FK-IDB-1K", "Idli and dosa batter", "Fresh kitchen", "1 kg", "VEG", "CHILLED", 3, false,
                "Ground this morning in the hub kitchen.", 85, 40, "K-01");
        add(hub, today, "FK-PSB-500", "Pesarattu batter", "Fresh kitchen", "500 g", "VEG", "CHILLED", 2, false,
                "Whole green moong, ginger, green chilli.", 70, 20, "K-02");
        add(hub, today, "FK-GPC-200", "Gongura pachadi", "Fresh kitchen", "200 g", "VEG", "CHILLED", 10, false,
                "Made in small batches.", 120, 18, "K-03");
        add(hub, today, "FK-CVM-400", "Cut vegetables, sambar mix", "Fresh kitchen", "400 g", "VEG", "CHILLED", 1, false,
                "Cut today. Use by tomorrow.", 60, 15, "K-04");
    }

    private void add(Hub hub, LocalDate today, String sku, String name, String category, String unit, String diet,
            String zone, Integer shelfLifeDays, boolean privateLabel, String description, int rupees, int stock,
            String bin) {
        Product product = products.save(
                new Product(sku, name, category, unit, diet, zone, shelfLifeDays, privateLabel, description));
        hubProducts.save(new HubProduct(hub.getId(), product.getId(), rupees * 100));
        if (stock > 0) {
            LocalDate useBy = shelfLifeDays == null ? null : today.plusDays(shelfLifeDays);
            inventory.receive(hub.getId(), product.getId(), stock, useBy, bin, null);
        }
    }
}
