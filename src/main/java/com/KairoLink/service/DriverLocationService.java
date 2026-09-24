package com.KairoLink.service;

import com.KairoLink.dto.DriverLocationRequest;
import com.KairoLink.dto.DriverLocationResponse;
import com.KairoLink.entity.DriverLocation;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.DriverLocationRepository;
import com.KairoLink.repository.BookingRepository;
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
    private final BookingRepository bookingRepository;

    public DriverLocationService(
            UserRepository userRepository,
            RideRepository rideRepository,
            DriverLocationRepository locationRepository,
            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.locationRepository = locationRepository;
        this.bookingRepository = bookingRepository;
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
        User user = findUser(email);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Current driver location was not found"));
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RideNotFoundException("Current driver location was not found");
        }
        boolean isDriver = ride.getDriver().getId().equals(user.getId());
        boolean isConfirmedRider = bookingRepository.existsByRiderIdAndRideIdAndStatus(
                user.getId(), rideId, BookingStatus.CONFIRMED);
        if (!isDriver && !isConfirmedRider) {
            throw new RideNotFoundException("Current driver location was not found");
        }
        return locationRepository.findByRideId(rideId)
                .map(this::toResponse)
                .orElseThrow(() -> new RideNotFoundException("Current driver location was not found"));
    }

    private Ride ongoingOwnedRide(String email, Long rideId) {
        Long driverId = findUser(email).getId();
        Ride ride = rideRepository.findByIdAndDriverId(rideId, driverId)
                .orElseThrow(() -> new RideNotFoundException("Ride was not found"));
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RideNotFoundException("Location is available only for ongoing rides");
        }
        return ride;
    }

    private User findUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
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
