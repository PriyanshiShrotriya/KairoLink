package com.KairoLink.service;

import com.KairoLink.dto.DriverLocationRequest;
import com.KairoLink.dto.DriverLocationResponse;
import com.KairoLink.entity.DriverLocation;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.DriverLocationRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class DriverLocationService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final DriverLocationRepository locationRepository;

    public DriverLocationService(
            UserRepository userRepository,
            RideRepository rideRepository,
            DriverLocationRepository locationRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public DriverLocationResponse update(String email, Long rideId, DriverLocationRequest request) {
        Ride ride = ongoingOwnedRide(email, rideId);
        validateCoordinates(request);
        DriverLocation location = locationRepository.findByRideId(rideId)
                .orElseGet(DriverLocation::new);
        location.setRide(ride);
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        return toResponse(locationRepository.save(location));
    }

    @Transactional
    public DriverLocationResponse getCurrent(String email, Long rideId) {
        ongoingOwnedRide(email, rideId);
        return locationRepository.findByRideId(rideId)
                .map(this::toResponse)
                .orElseThrow(() -> new RideNotFoundException("Current driver location was not found"));
    }

    private Ride ongoingOwnedRide(String email, Long rideId) {
        String normalizedEmail = normalizeEmail(email);
        Long driverId = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"))
                .getId();
        Ride ride = rideRepository.findByIdAndDriverId(rideId, driverId)
                .orElseThrow(() -> new RideNotFoundException("Ride was not found"));
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RideNotFoundException("Location is available only for ongoing rides");
        }
        return ride;
    }

    private void validateCoordinates(DriverLocationRequest request) {
        if (request == null || request.latitude() == null || request.longitude() == null
                || !within(request.latitude(), -90, 90)
                || !within(request.longitude(), -180, 180)) {
            throw new IllegalArgumentException("Driver coordinates are invalid");
        }
    }

    private boolean within(BigDecimal value, int minimum, int maximum) {
        return value.compareTo(BigDecimal.valueOf(minimum)) >= 0
                && value.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }

    private DriverLocationResponse toResponse(DriverLocation location) {
        return new DriverLocationResponse(
                location.getLatitude(), location.getLongitude(), location.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
