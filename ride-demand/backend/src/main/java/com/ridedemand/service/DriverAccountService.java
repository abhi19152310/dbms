package com.ridedemand.service;

import com.ridedemand.model.DriverAccount;
import com.ridedemand.repository.DriverAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DriverAccountService {

    private final DriverAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DriverAccountService(DriverAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public DriverAccount register(String username, String password) {
        String normalizedUsername = username.trim();
        if (repository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already registered");
        }
        return repository.save(new DriverAccount(normalizedUsername, passwordEncoder.encode(password)));
    }
}
