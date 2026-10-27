package com.ridedemand.controller;

import com.ridedemand.dto.Dtos.ObservationRequest;
import com.ridedemand.dto.Dtos.RideFoundRequest;
import com.ridedemand.dto.Dtos.TrainingResponse;
import com.ridedemand.dto.Dtos.UnmetDemandRequest;
import com.ridedemand.service.DataCollectionService;
import com.ridedemand.service.TrainingService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
public class DataCollectionController {

    private final DataCollectionService service;
    private final TrainingService trainingService;

    public DataCollectionController(DataCollectionService service, TrainingService trainingService) {
        this.service = service;
        this.trainingService = trainingService;
    }

    @PostMapping("/reports/unmet-demand")
    public ResponseEntity<Map<String, Object>> reportUnmetDemand(
            Authentication authentication,
            @Valid @RequestBody UnmetDemandRequest request) {

        var saved = service.saveReport(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "status", saved.getStatus()));
    }

    @PostMapping("/observations")
    public ResponseEntity<Map<String, Object>> addObservation(
            Authentication authentication, @Valid @RequestBody ObservationRequest request) {
        var saved = service.saveObservation(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId()));
    }

    @PostMapping("/observations/ride-found")
    public ResponseEntity<Map<String, Object>> reportRideFound(
            Authentication authentication, @Valid @RequestBody RideFoundRequest request) {
        var saved = service.saveRideFound(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId()));
    }

    @PostMapping("/training/run")
    public TrainingResponse trainPendingData() {
        return trainingService.trainPendingData();
    }
}
