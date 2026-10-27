package com.ridedemand.repository;

import com.ridedemand.model.DriverObservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverObservationRepository extends JpaRepository<DriverObservation, Long> {
}
