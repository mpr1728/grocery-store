package com.grocerystore.api.passkey;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasskeyCeremonyRepository extends JpaRepository<PasskeyCeremony, Long> {

    Optional<PasskeyCeremony> findByTokenHash(String tokenHash);
}
