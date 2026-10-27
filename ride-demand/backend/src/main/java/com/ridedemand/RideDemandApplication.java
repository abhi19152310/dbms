package com.ridedemand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RideDemandApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideDemandApplication.class, args);
    }
}
