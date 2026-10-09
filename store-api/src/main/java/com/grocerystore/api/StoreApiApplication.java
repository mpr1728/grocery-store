package com.grocerystore.api;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import com.grocerystore.api.common.StoreProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreApiApplication.class, args);
    }

    // Use-by dates are compared against "today" in the store's own time zone.
    @Bean
    public Clock clock(StoreProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
