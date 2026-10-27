package com.ridedemand.service;

import com.ridedemand.dto.Dtos.ObservationRequest;
import com.ridedemand.dto.Dtos.RideFoundRequest;
import com.ridedemand.dto.Dtos.UnmetDemandRequest;
import com.ridedemand.model.DriverObservation;
import com.ridedemand.model.UnmetDemandReport;
import com.ridedemand.repository.DriverObservationRepository;
import com.ridedemand.repository.UnmetDemandReportRepository;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// stores the new data that keeps the model improving
@Service
public class DataCollectionService {

    private final UnmetDemandReportRepository reportRepository;
    private final DriverObservationRepository observationRepository;

    public DataCollectionService(UnmetDemandReportRepository reportRepository,
                                 DriverObservationRepository observationRepository) {
        this.reportRepository = reportRepository;
        this.observationRepository = observationRepository;
    }

    public UnmetDemandReport saveReport(String userId, UnmetDemandRequest req) {
        // max one report per rider per day
        if (reportRepository.existsByUserIdAndReportDate(userId, LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You already sent a report today");
        }
        return reportRepository.save(new UnmetDemandReport(userId, req.area(), req.vehicleType()));
    }

    public DriverObservation saveObservation(String driverId, ObservationRequest req) {
        // we only store data from drivers who said yes
        if (!req.consent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver has not given consent");
        }
        return observationRepository.save(new DriverObservation(
                driverId, req.area(), req.vehicleType(), req.online(), req.rideOutcome(),
                req.latitude(), req.longitude()));
    }

    public DriverObservation saveRideFound(String driverId, RideFoundRequest req) {
        if (!req.consent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver has not given consent");
        }
        return observationRepository.save(new DriverObservation(
                driverId, null, req.vehicleType(), true, "FOUND_RIDE",
                req.latitude(), req.longitude()));
    }
}
