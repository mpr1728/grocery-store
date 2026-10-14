package com.grocerystore.api.fashion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FashionOrderRepository extends JpaRepository<FashionOrder, Long> {

    Optional<FashionOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<FashionOrder> findTop50ByCustomerIdOrderByIdDesc(Long customerId);

    List<FashionOrder> findTop100ByStatusInOrderByIdDesc(Collection<FashionOrderStatus> statuses);
}
