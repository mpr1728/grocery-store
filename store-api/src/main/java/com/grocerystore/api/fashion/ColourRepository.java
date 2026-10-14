package com.grocerystore.api.fashion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ColourRepository extends JpaRepository<Colour, String> {

    List<Colour> findAllByOrderBySortOrder();
}
