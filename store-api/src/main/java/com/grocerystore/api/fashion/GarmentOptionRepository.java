package com.grocerystore.api.fashion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentOptionRepository extends JpaRepository<GarmentOption, Long> {

    List<GarmentOption> findAllByOrderByGarmentCodeAscPartAscSortOrderAsc();

    List<GarmentOption> findByGarmentCode(String garmentCode);
}
