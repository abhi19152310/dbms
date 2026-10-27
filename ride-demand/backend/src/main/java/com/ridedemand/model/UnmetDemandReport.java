package com.ridedemand.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// a rider coult not find a driver
@Entity
@Table(name = "unmet_demand_reports")
public class UnmetDemandReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String area;
    private String vehicleType;
    private LocalDateTime reportedAt;
    private LocalDate reportDate;   // used for the "once per day" check
    private String status;          // PENDING -> VERIFIED / REJECTED
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean usedForTraining;

    public UnmetDemandReport() {}

    public UnmetDemandReport(String userId, String area, String vehicleType) {
        this.userId = userId;
        this.area = area;
        this.vehicleType = vehicleType;
        this.reportedAt = LocalDateTime.now();
        this.reportDate = LocalDate.now();
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getArea() { return area; }
    public String getVehicleType() { return vehicleType; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public LocalDate getReportDate() { return reportDate; }
    public String getStatus() { return status; }
    public boolean isUsedForTraining() { return usedForTraining; }
    public void markUsedForTraining() { this.usedForTraining = true; }
}
