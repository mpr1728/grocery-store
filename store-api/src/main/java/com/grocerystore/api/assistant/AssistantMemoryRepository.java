package com.grocerystore.api.assistant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantMemoryRepository extends JpaRepository<AssistantMemory, Long> {

    List<AssistantMemory> findByCustomerIdOrderById(Long customerId);

    @Modifying
    @Query("delete from AssistantMemory m where m.customerId = :customerId")
    void deleteAllFor(@Param("customerId") Long customerId);
}
