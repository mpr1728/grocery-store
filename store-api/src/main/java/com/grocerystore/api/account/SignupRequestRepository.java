package com.grocerystore.api.account;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupRequestRepository extends JpaRepository<SignupRequest, Long> {

    Optional<SignupRequest> findByTokenHash(String tokenHash);

    long countByEmailAndCreatedAtAfter(String email, Instant since);
}
