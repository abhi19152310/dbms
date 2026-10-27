package com.ridedemand.service;

import com.ridedemand.dto.Dtos.MlItem;
import com.ridedemand.dto.Dtos.MlPrediction;
import com.ridedemand.dto.Dtos.MlRequest;
import com.ridedemand.dto.Dtos.MlResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

// calls the Python ML service
@Service
public class PredictionService {

    private final RestClient client;

    public PredictionService(@Value("${ml.service.url}") String mlUrl) {
        this.client = RestClient.builder().baseUrl(mlUrl).build();
    }

    public List<MlPrediction> predict(List<MlItem> items) {
        MlResponse response;
        try {
            response = client.post()
                    .uri("/predict")
                    .body(new MlRequest(items))
                    .retrieve()
                    .body(MlResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ML service is not reachable");
        }

        if (response == null || response.predictions().size() != items.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "ML service returned something unexpected");
        }
        return response.predictions();
    }

}
