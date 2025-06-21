package com.grocerystore.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/products")
public class ProductControllerProxy {

    private final RestTemplate restTemplate;

    @Value("${backend.product-service.url}")
    private String productServiceUrl;

    public ProductControllerProxy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<String> getAllProducts() {
        return restTemplate.getForEntity(productServiceUrl + "/products", String.class);
    }

    @PostMapping
    public ResponseEntity<String> addProduct(@RequestBody String product) {
        return restTemplate.postForEntity(productServiceUrl + "/products", product, String.class);
    }
}
