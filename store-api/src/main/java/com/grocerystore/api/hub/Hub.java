package com.grocerystore.api.hub;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hubs")
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private String city;
    @Column(name = "service_radius_m")
    private int serviceRadiusM;
    private boolean active = true;

    protected Hub() {
    }

    public Hub(String code, String name, String city, int serviceRadiusM) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.serviceRadiusM = serviceRadiusM;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public int getServiceRadiusM() {
        return serviceRadiusM;
    }

    public boolean isActive() {
        return active;
    }
}
