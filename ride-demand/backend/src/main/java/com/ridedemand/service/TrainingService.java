package com.ridedemand.service;

import com.ridedemand.dto.Dtos.TrainingRequest;
import com.ridedemand.dto.Dtos.TrainingResponse;
import com.ridedemand.dto.Dtos.TrainingSample;
import com.ridedemand.model.DriverObservation;
import com.ridedemand.model.UnmetDemandReport;
import com.ridedemand.repository.DriverObservationRepository;
import com.ridedemand.repository.UnmetDemandReportRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrainingService {

    private final UnmetDemandReportRepository reportRepository;
    private final DriverObservationRepository observationRepository;
    private final LocationService locationService;
    private final RestClient mlClient;

    public TrainingService(UnmetDemandReportRepository reportRepository,
                           DriverObservationRepository observationRepository,
                           LocationService locationService,
                           @Value("${ml.service.url}") String mlUrl) {
        this.reportRepository = reportRepository;
        this.observationRepository = observationRepository;
        this.locationService = locationService;
        this.mlClient = RestClient.builder().baseUrl(mlUrl).build();
    }

    @Scheduled(fixedDelay = 60_000)
    public void trainWhenBatchReady() {
        long pending = reportRepository.findAll().stream()
                .filter(report -> !report.isUsedForTraining() && isEligible(report))
                .count()
                + observationRepository.findAll().stream()
                .filter(observation -> !observation.isUsedForTraining()
                        && customerCount(observation.getRideOutcome()) != null)
                .count();
        if (pending >= 10) {
            trainPendingData();
        }
    }

    public synchronized TrainingResponse trainPendingData() {
        List<UnmetDemandReport> reports = reportRepository.findAll();
        List<DriverObservation> observations = observationRepository.findAll();
        List<TrainingSample> samples = new ArrayList<>();
        List<UnmetDemandReport> trainedReports = new ArrayList<>();
        List<DriverObservation> trainedObservations = new ArrayList<>();

        for (UnmetDemandReport report : reports) {
            TrainingSample sample = reportSample(report);
            if (sample != null) {
                samples.add(sample);
                if (!report.isUsedForTraining()) {
                    trainedReports.add(report);
                }
            }
        }

        for (DriverObservation observation : observations) {
            TrainingSample sample = observationSample(observation);
            if (sample != null) {
                samples.add(sample);
                if (!observation.isUsedForTraining()) {
                    trainedObservations.add(observation);
                }
            }
        }

        if (trainedReports.isEmpty() && trainedObservations.isEmpty()) {
            return new TrainingResponse("no_new_samples", 0);
        }

        TrainingResponse response;
        try {
            response = mlClient.post()
                    .uri("/train")
                    .body(new TrainingRequest(samples))
                    .retrieve()
                    .body(TrainingResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "ML training failed; collected data was not marked as used", e);
        }

        if (response == null || !"trained".equals(response.status())
                || response.samplesUsed() != samples.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "ML service returned an unexpected training result; collected data was not marked as used");
        }

        trainedReports.forEach(UnmetDemandReport::markUsedForTraining);
        trainedObservations.forEach(DriverObservation::markUsedForTraining);
        reportRepository.saveAll(trainedReports);
        observationRepository.saveAll(trainedObservations);
        return response;
    }

    private boolean isEligible(UnmetDemandReport report) {
        return !"REJECTED".equalsIgnoreCase(report.getStatus());
    }

    private Integer customerCount(String outcome) {
        if (outcome == null) {
            return null;
        }
        return switch (outcome.trim().toUpperCase(Locale.ROOT)) {
            case "FOUND_RIDE" -> 1;
            case "NO_RIDE" -> 0;
            default -> null;
        };
    }

    private TrainingSample reportSample(UnmetDemandReport report) {
        if (!isEligible(report)) {
            return null;
        }
        return sample(report.getArea(), report.getVehicleType(), report.getReportedAt(), 1);
    }

    private TrainingSample observationSample(DriverObservation observation) {
        Integer customers = customerCount(observation.getRideOutcome());
        if (customers == null) {
            return null;
        }

        String area = observation.getArea();
        if (area == null || area.isBlank()) {
            if (observation.getLatitude() == null || observation.getLongitude() == null) {
                return null;
            }
            var nearest = locationService.findNearest(observation.getLatitude(), observation.getLongitude());
            if (nearest == null) {
                return null;
            }
            area = nearest.area().getName();
        }
        return sample(area, observation.getVehicleType(), observation.getObservedAt(), customers);
    }

    private TrainingSample sample(String area, String vehicleType,
                                  LocalDateTime observedAt, int customers) {
        if (area == null || area.isBlank() || vehicleType == null || vehicleType.isBlank()
                || observedAt == null) {
            return null;
        }
        return new TrainingSample(area.trim(), vehicleType.trim(), observedAt, customers);
    }
}
