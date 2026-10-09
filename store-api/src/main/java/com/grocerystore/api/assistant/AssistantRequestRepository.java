package com.grocerystore.api.assistant;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantRequestRepository extends JpaRepository<AssistantRequest, Long> {

    long countByCustomerIdAndCreatedAtAfter(Long customerId, Instant since);
}
