package com.ridedemand.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// one observation shared by a driver
@Entity
@Table(name = "driver_observations")
public class DriverObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String driverId;
    private String area;
    private String vehicleType;
    private boolean online;
    private String rideOutcome;
    private Double latitude;
    private Double longitude;

    private LocalDateTime observedAt;
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean usedForTraining;

    public DriverObservation() {}

    public DriverObservation(String driverId, String area, String vehicleType, boolean online,
                             String rideOutcome, Double latitude, Double longitude) {
        this.driverId = driverId;
        this.area = area;
        this.vehicleType = vehicleType;
        this.online = online;
        this.rideOutcome = rideOutcome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.observedAt = LocalDateTime.now();
    }

    public DriverObservation(String driverId, String area, String vehicleType, boolean online) {
        this(driverId, area, vehicleType, online, null, null, null);
    }

    public Long getId() { return id; }
    public String getDriverId() { return driverId; }
    public String getArea() { return area; }
    public String getVehicleType() { return vehicleType; }
    public boolean isOnline() { return online; }
    public String getRideOutcome() { return rideOutcome; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public boolean isUsedForTraining() { return usedForTraining; }
    public void markUsedForTraining() { this.usedForTraining = true; }
}
