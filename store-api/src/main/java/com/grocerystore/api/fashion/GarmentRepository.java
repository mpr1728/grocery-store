package com.grocerystore.api.fashion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentRepository extends JpaRepository<Garment, String> {

    List<Garment> findByActiveTrueOrderBySortOrder();
}
