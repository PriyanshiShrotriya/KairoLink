package com.KairoLink.service;

import com.KairoLink.dto.RideRequest;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class RideService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;

    public RideService(UserRepository userRepository, RideRepository rideRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
    }

    @Transactional
    public Ride publish(String email, RideRequest request) {
        User driver = findDriver(email);
        validateRoute(request);
        Ride ride = new Ride();
        ride.setDriver(driver);
        copyRequest(ride, request);
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.save(ride);
    }

    public List<Ride> findMyRides(String email) {
        User driver = findDriver(email);
        return rideRepository.findByDriverIdOrderByDepartureTimeAsc(driver.getId());
    }

    public Ride getMyRide(String email, Long rideId) {
        User driver = findDriver(email);
        return rideRepository.findByIdAndDriverId(rideId, driver.getId())
                .orElseThrow(() -> new RideNotFoundException("Ride was not found"));
    }

    @Transactional
    public Ride update(String email, Long rideId, RideRequest request) {
        Ride ride = getEditableRide(email, rideId);
        validateRoute(request);
        copyRequest(ride, request);
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride cancel(String email, Long rideId) {
        Ride ride = getEditableRide(email, rideId);
        ride.setStatus(RideStatus.CANCELLED);
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride start(String email, Long rideId) {
        Ride ride = getMyRide(email, rideId);
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new RideNotFoundException("Ride cannot be started");
        }
        ride.setStatus(RideStatus.ONGOING);
        return rideRepository.save(ride);
    }

    @Transactional
    public Ride complete(String email, Long rideId) {
        Ride ride = getMyRide(email, rideId);
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new RideNotFoundException("Ride cannot be completed");
        }
        ride.setStatus(RideStatus.COMPLETED);
        return rideRepository.save(ride);
    }

    private Ride getEditableRide(String email, Long rideId) {
        Ride ride = getMyRide(email, rideId);
        if (ride.getStatus() != RideStatus.ACTIVE
                || !ride.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new RideNotFoundException("Ride can no longer be changed");
        }
        return ride;
    }

    private User findDriver(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
        if (!user.getRoles().contains(Role.DRIVER)) {
            throw new RideNotFoundException("Driver ride was not found");
        }
        return user;
    }

    private void copyRequest(Ride ride, RideRequest request) {
        ride.setSource(request.getSource().trim());
        ride.setDestination(request.getDestination().trim());
        ride.setDepartureTime(request.getDepartureTime());
        ride.setSeats(request.getSeats());
        ride.setPrice(request.getPrice());
    }

    private void validateRoute(RideRequest request) {
        if (request.getSource().trim().equalsIgnoreCase(request.getDestination().trim())) {
            throw new IllegalArgumentException("Source and destination must be different");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
