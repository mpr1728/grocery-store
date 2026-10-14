package com.grocerystore.api.fashion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FabricRepository extends JpaRepository<Fabric, String> {

    List<Fabric> findByActiveTrueOrderBySortOrder();
}
