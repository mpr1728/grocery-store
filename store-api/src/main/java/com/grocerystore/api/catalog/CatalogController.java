package com.grocerystore.api.catalog;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.hub.HubService;

@RestController
@RequestMapping("/api/hubs/{hubId}/catalog")
public class CatalogController {

    private final HubService hubs;
    private final HubProductRepository hubProducts;
    private final Clock clock;

    public CatalogController(HubService hubs, HubProductRepository hubProducts, Clock clock) {
        this.hubs = hubs;
        this.hubProducts = hubProducts;
        this.clock = clock;
    }

    @GetMapping
    public List<CatalogItem> catalog(@PathVariable Long hubId, @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        hubs.requireActive(hubId);
        String categoryFilter = isBlank(category) ? null : category.trim();
        String namePattern = isBlank(q) ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return hubProducts.findCatalog(hubId, LocalDate.now(clock), categoryFilter, namePattern);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
