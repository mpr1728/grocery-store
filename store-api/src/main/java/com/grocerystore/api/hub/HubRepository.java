package com.grocerystore.api.hub;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HubRepository extends JpaRepository<Hub, Long> {

    List<Hub> findByActiveTrueOrderByName();
}
