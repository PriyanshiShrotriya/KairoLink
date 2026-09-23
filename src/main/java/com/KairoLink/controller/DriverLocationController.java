package com.KairoLink.controller;

import com.KairoLink.dto.DriverLocationRequest;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.service.DriverLocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rides/{rideId}/driver-location")
public class DriverLocationController {

    private final DriverLocationService locationService;

    public DriverLocationController(DriverLocationService locationService) {
        this.locationService = locationService;
    }

    @PutMapping
    public ResponseEntity<?> update(
            Authentication authentication,
            @PathVariable Long rideId,
            @Valid @RequestBody DriverLocationRequest request) {
        try {
            return ResponseEntity.ok(
                    locationService.update(authentication.getName(), rideId, request));
        } catch (RideNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getCurrent(
            Authentication authentication,
            @PathVariable Long rideId) {
        try {
            return ResponseEntity.ok(
                    locationService.getCurrent(authentication.getName(), rideId));
        } catch (RideNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
