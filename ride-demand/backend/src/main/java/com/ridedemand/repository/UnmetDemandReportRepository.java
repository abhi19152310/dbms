package com.ridedemand.repository;

import com.ridedemand.model.UnmetDemandReport;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnmetDemandReportRepository extends JpaRepository<UnmetDemandReport, Long> {

    boolean existsByUserIdAndReportDate(String userId, LocalDate reportDate);
}
