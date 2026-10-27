package com.ridedemand.service;

import com.ridedemand.dto.Dtos.AreaDemand;
import com.ridedemand.dto.Dtos.MlItem;
import com.ridedemand.dto.Dtos.MlPrediction;
import com.ridedemand.dto.Dtos.PredictRequest;
import com.ridedemand.dto.Dtos.PredictResponse;
import com.ridedemand.service.LocationService.Nearby;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DemandService {

    private final LocationService locationService;
    private final PredictionService predictionService;
    private final double defaultRadiusKm;

    public DemandService(LocationService locationService,
                         PredictionService predictionService,
                         @Value("${demand.default-radius-km}") double defaultRadiusKm) {
        this.locationService = locationService;
        this.predictionService = predictionService;
        this.defaultRadiusKm = defaultRadiusKm;
    }

    public PredictResponse predictNearby(PredictRequest req) {
        LocalTime time;
        LocalDate date;
        try {
            time = LocalTime.parse(req.time());
            date = (req.date() == null || req.date().isBlank()) ? LocalDate.now() : LocalDate.parse(req.date());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use time like 19:00 and date like 2024-06-14");
        }

        double radius = req.radiusKm() != null ? req.radiusKm() : defaultRadiusKm;

        // 1. find nearby areas
        List<Nearby> nearby = locationService.findNearby(req.latitude(), req.longitude(), radius);
        if (nearby.isEmpty()) {
            return new PredictResponse(List.of());
        }

        // 2. same features for every area, predict in one call
        List<MlItem> items = new ArrayList<>();
        for (Nearby n : nearby) {
            // Java's Monday is 1, pandas' Monday is 0, hence the -1
            items.add(new MlItem(n.area().getName(), req.vehicle(), time.getHour(),
                    date.getDayOfWeek().getValue() - 1, date.getMonthValue()));
        }
        List<MlPrediction> predictions = predictionService.predict(items);

        // 3. combine and rank
        List<AreaDemand> results = new ArrayList<>();
        for (int i = 0; i < nearby.size(); i++) {
            Nearby n = nearby.get(i);
            MlPrediction p = predictions.get(i);
            double distance = Math.round(n.distanceKm() * 10) / 10.0;
            results.add(new AreaDemand(n.area().getName(), n.area().getLatitude(), n.area().getLongitude(),
                    distance, p.expectedCustomers(), p.customerProbability()));
        }

            results.sort(Comparator.comparingDouble(AreaDemand::customerProbability).reversed()
                .thenComparing(Comparator.comparingDouble(AreaDemand::expectedCustomers).reversed()));
        return new PredictResponse(results);
    }
}
