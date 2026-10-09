package com.grocerystore.api.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByReferenceOrderById(String reference);
}
