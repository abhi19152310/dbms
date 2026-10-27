package com.ridedemand.service;

import com.ridedemand.model.Area;
import com.ridedemand.repository.AreaRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

// turns one driver position into a list of nearby pickup areas
@Service
public class LocationService {

    public record Nearby(Area area, double distanceKm) {
    }

    private final AreaRepository areaRepository;

    public LocationService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public List<Nearby> findNearby(double lat, double lng, double radiusKm) {
        List<Nearby> result = new ArrayList<>();

        
        for (Area area : areaRepository.findAll()) {
            double distance = distanceKm(lat, lng, area.getLatitude(), area.getLongitude());
            if (distance <= radiusKm) {
                result.add(new Nearby(area, distance));
            }
        }

        result.sort(Comparator.comparingDouble(Nearby::distanceKm));
        return result;
    }

    public Nearby findNearest(double lat, double lng) {
        return areaRepository.findAll().stream()
                .map(area -> new Nearby(area, distanceKm(lat, lng, area.getLatitude(), area.getLongitude())))
                .min(Comparator.comparingDouble(Nearby::distanceKm))
                .orElse(null);
    }

    // haversine formula: distance between two lat/lng points
    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371;
        double dLat = Math.toRadians(lat2 - lat1);//Δϕ=ϕ2​−ϕ1​ Converting degrees to radians
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);// Formula based on-> Haversine formula: a = sin²(Δϕ/2) + cos ϕ1 ⋅ cos ϕ2 ⋅ sin²(Δλ/2)

        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));// Formula based on-> c = 2 ⋅ atan2( √a, √(1−a) ) d = R ⋅ c
    }
}
