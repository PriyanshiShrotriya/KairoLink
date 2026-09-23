package com.KairoLink.service;

import com.KairoLink.dto.DriverLocationRequest;
import com.KairoLink.dto.DriverLocationResponse;
import com.KairoLink.entity.DriverLocation;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.repository.DriverLocationRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DriverLocationServiceTest {

    private UserRepository userRepository;
    private RideRepository rideRepository;
    private DriverLocationRepository locationRepository;
    private DriverLocationService locationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        rideRepository = mock(RideRepository.class);
        locationRepository = mock(DriverLocationRepository.class);
        locationService = new DriverLocationService(
                userRepository, rideRepository, locationRepository);
    }

    @Test
    void updatesAndPersistsLocationForOwningOngoingRide() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, RideStatus.ONGOING);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));
        when(locationRepository.findByRideId(1L)).thenReturn(Optional.empty());
        when(locationRepository.save(any(DriverLocation.class))).thenAnswer(invocation -> {
            DriverLocation location = invocation.getArgument(0);
            location.setUpdatedAt(java.time.Instant.now());
            return location;
        });

        DriverLocationResponse response = locationService.update(
                driver.getEmail(), 1L,
                new DriverLocationRequest(new BigDecimal("28.613900"), new BigDecimal("77.209000")));

        assertEquals(new BigDecimal("28.613900"), response.latitude());
        assertEquals(new BigDecimal("77.209000"), response.longitude());
        verify(locationRepository).save(any(DriverLocation.class));
    }

    @Test
    void rejectsNonOwnerAndNonOngoingRide() {
        User driver = driver("driver@example.com");
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () -> locationService.update(
                driver.getEmail(), 1L,
                new DriverLocationRequest(BigDecimal.ZERO, BigDecimal.ZERO)));

        Ride activeRide = ride(driver, RideStatus.ACTIVE);
        when(rideRepository.findByIdAndDriverId(2L, driver.getId()))
                .thenReturn(Optional.of(activeRide));
        assertThrows(RideNotFoundException.class, () -> locationService.update(
                driver.getEmail(), 2L,
                new DriverLocationRequest(BigDecimal.ZERO, BigDecimal.ZERO)));
        verifyNoInteractions(locationRepository);
    }

    @Test
    void rejectsCoordinatesOutsideValidRanges() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, RideStatus.ONGOING);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));

        assertThrows(IllegalArgumentException.class, () -> locationService.update(
                driver.getEmail(), 1L,
                new DriverLocationRequest(new BigDecimal("90.1"), BigDecimal.ZERO)));
        verifyNoInteractions(locationRepository);
    }

    private User driver(String email) {
        User driver = new User();
        driver.setId(7L);
        driver.setEmail(email);
        driver.setRoles(Set.of(com.KairoLink.entity.Role.DRIVER));
        return driver;
    }

    private Ride ride(User driver, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setDriver(driver);
        ride.setStatus(status);
        return ride;
    }
}
