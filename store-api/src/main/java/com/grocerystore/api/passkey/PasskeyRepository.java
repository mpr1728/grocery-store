package com.grocerystore.api.passkey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasskeyRepository extends JpaRepository<Passkey, Long> {

    List<Passkey> findByCustomerIdOrderById(Long customerId);

    Optional<Passkey> findByCredentialId(String credentialId);
}
