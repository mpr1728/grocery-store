package com.grocerystore.api.cart;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartLineRepository extends JpaRepository<CartLine, Long> {

    List<CartLine> findByCustomerIdOrderById(Long customerId);

    // Runs at once, so the lines that replace these can be inserted in the same transaction.
    @Modifying
    @Query("delete from CartLine c where c.customerId = :customerId")
    void deleteAllFor(@Param("customerId") Long customerId);
}
