package com.grocerystore.api.hub;

import java.util.List;

import org.springframework.stereotype.Service;

import com.grocerystore.api.common.NotFoundException;

@Service
public class HubService {

    private final HubRepository hubs;

    public HubService(HubRepository hubs) {
        this.hubs = hubs;
    }

    public List<Hub> activeHubs() {
        return hubs.findByActiveTrueOrderByName();
    }

    public Hub requireActive(Long hubId) {
        return hubs.findById(hubId)
                .filter(Hub::isActive)
                .orElseThrow(() -> new NotFoundException("Hub " + hubId + " was not found"));
    }
}
