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

        // Everyday basics that most recipes need.
        add(hub, today, "FV-GIN-100", "Ginger", "Fruit and vegetables", "100 g", "VEG", "PRODUCE", 14, false,
                "Fresh, firm roots.", 15, 40, "P-06");
        add(hub, today, "FV-GCH-100", "Green chilli", "Fruit and vegetables", "100 g", "VEG", "PRODUCE", 7, false,
                "Medium hot.", 10, 40, "P-07");
        add(hub, today, "FV-GAR-200", "Garlic", "Fruit and vegetables", "200 g", "VEG", "PRODUCE", 30, false,
                "Whole bulbs.", 40, 40, "P-08");
        add(hub, today, "FV-COR-100", "Coriander leaves", "Fruit and vegetables", "100 g", "VEG", "PRODUCE", 3, false,
                "Cleaned bunch.", 15, 30, "P-09");
        add(hub, today, "FV-CUL-50", "Curry leaves", "Fruit and vegetables", "50 g", "VEG", "PRODUCE", 5, false,
                "Fresh sprigs.", 10, 30, "P-10");
        add(hub, today, "FV-POT-1K", "Potato", "Fruit and vegetables", "1 kg", "VEG", "PRODUCE", 21, false,
                "Medium size.", 35, 60, "P-11");
        add(hub, today, "FV-LEM-4", "Lemon", "Fruit and vegetables", "4 pieces", "VEG", "PRODUCE", 10, false,
                "Juicy, thin skinned.", 20, 40, "P-12");
        add(hub, today, "FV-CAR-500", "Carrot", "Fruit and vegetables", "500 g", "VEG", "PRODUCE", 7, false,
                "Orange, washed.", 30, 40, "P-13");
        add(hub, today, "ST-RIC-1K", "Sona Masoori rice", "Staples", "1 kg", "VEG", "AMBIENT", null, true,
                "Aged 12 months. House brand.", 85, 50, "A-06");
        add(hub, today, "ST-ATT-1K", "Whole wheat atta", "Staples", "1 kg", "VEG", "AMBIENT", null, true,
                "Stone ground chakki atta. House brand.", 60, 50, "A-07");
        add(hub, today, "ST-CUP-100", "Cumin powder", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Ground in small batches. House brand.", 60, 40, "A-08");
        add(hub, today, "ST-COP-100", "Coriander powder", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Ground in small batches. House brand.", 40, 40, "A-09");
        add(hub, today, "ST-RCP-100", "Red chilli powder", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Tested for added colour. House brand.", 50, 40, "A-10");
        add(hub, today, "ST-GMS-100", "Garam masala", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Whole spices, roasted and ground. House brand.", 75, 30, "A-11");
        add(hub, today, "ST-MUS-100", "Mustard seeds", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Small black seeds. House brand.", 25, 40, "A-12");
        add(hub, today, "ST-CUM-100", "Cumin seeds", "Staples", "100 g", "VEG", "AMBIENT", null, true,
                "Cleaned and sorted. House brand.", 55, 40, "A-13");
        add(hub, today, "ST-SLT-1K", "Salt, iodised", "Staples", "1 kg", "VEG", "AMBIENT", null, true,
                "Free flowing. House brand.", 25, 60, "A-14");
        add(hub, today, "ST-SGR-1K", "Sugar", "Staples", "1 kg", "VEG", "AMBIENT", null, true,
                "Sulphur free. House brand.", 48, 60, "A-15");
        add(hub, today, "ST-TAM-200", "Tamarind", "Staples", "200 g", "VEG", "AMBIENT", null, true,
                "Seedless. House brand.", 45, 30, "A-16");
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
