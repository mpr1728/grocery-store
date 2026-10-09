package com.grocerystore.api.hub;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hubs")
public class HubController {

    public record HubView(Long id, String code, String name, String city, int serviceRadiusM) {
    }

    private final HubService hubs;

    public HubController(HubService hubs) {
        this.hubs = hubs;
    }

    @GetMapping
    public List<HubView> list() {
        return hubs.activeHubs().stream()
                .map(h -> new HubView(h.getId(), h.getCode(), h.getName(), h.getCity(), h.getServiceRadiusM()))
                .toList();
    }
}
