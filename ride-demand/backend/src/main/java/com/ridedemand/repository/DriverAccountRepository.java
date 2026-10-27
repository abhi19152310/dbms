package com.ridedemand.repository;

import com.ridedemand.model.DriverAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverAccountRepository extends JpaRepository<DriverAccount, Long> {

    Optional<DriverAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
