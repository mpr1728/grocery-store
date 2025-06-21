package com.grocerystore.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/users")
public class UserControllerProxy {

    private final RestTemplate restTemplate;

    @Value("${backend.user-service.url}")
    private String userServiceUrl;

    public UserControllerProxy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody String user) {
        return restTemplate.postForEntity(userServiceUrl + "/register", user, String.class);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody String user) {
        return restTemplate.postForEntity(userServiceUrl + "/login", user, String.class);
    }
}
