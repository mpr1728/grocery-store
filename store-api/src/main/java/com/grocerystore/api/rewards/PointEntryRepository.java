package com.grocerystore.api.rewards;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PointEntryRepository extends JpaRepository<PointEntry, Long> {

    List<PointEntry> findTop20ByCustomerIdOrderByIdDesc(Long customerId);
}
