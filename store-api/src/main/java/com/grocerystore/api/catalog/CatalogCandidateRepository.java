package com.grocerystore.api.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCandidateRepository extends JpaRepository<CatalogCandidate, Long> {

    List<CatalogCandidate> findTop100ByStatusAndIdGreaterThanOrderById(String status, Long id);

    long countByStatus(String status);
}
