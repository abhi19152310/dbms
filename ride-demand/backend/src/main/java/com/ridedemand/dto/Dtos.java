package com.ridedemand.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;


public class Dtos {

    public record AuthRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record AuthResponse(String username, String role) {
    }

    // ---- what the React app sends / receives ----
    public record PredictRequest(
            @NotNull Double latitude,
            @NotNull Double longitude,
            @NotBlank String vehicle,
            @NotBlank String time,     
            String date,                
            Double radiusKm) {          
    }

    public record AreaDemand(
            String area,
            double latitude,
            double longitude,
            double distanceKm,
            double expectedCustomers,
            double customerProbability) {
    }

    public record PredictResponse(List<AreaDemand> areas) {
    }

    public record UnmetDemandRequest(
            @NotBlank String area,
            @NotBlank String vehicleType) {
    }

    public record ObservationRequest(
            @NotBlank String area,
            @NotBlank String vehicleType,
            boolean online,
            String rideOutcome,
            boolean consent,
            Double latitude,
            Double longitude) {
    }

    public record RideFoundRequest(
            @NotBlank String vehicleType,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            boolean consent) {
    }

    // ---- what we send to / get from the Python ML service ----
    public record MlItem(
            String area,
            String vehicle,
            int hour,
            @JsonProperty("day_of_week") int dayOfWeek,
            int month) {
    }

    public record MlRequest(List<MlItem> items) {
    }

    public record MlPrediction(
            @JsonProperty("expected_customers") double expectedCustomers,
            @JsonProperty("customer_probability") double customerProbability) {
    }

    public record MlResponse(List<MlPrediction> predictions) {
    }

    public record TrainingSample(
            String area,
            String vehicleType,
            LocalDateTime observedAt,
            int customers) {
    }

    public record TrainingRequest(List<TrainingSample> samples) {
    }

    public record TrainingResponse(String status, int samplesUsed) {
    }
}
