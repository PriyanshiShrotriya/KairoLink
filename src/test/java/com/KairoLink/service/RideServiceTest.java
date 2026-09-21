package com.KairoLink.service;

import com.KairoLink.dto.RideRequest;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RideServiceTest {

    private UserRepository userRepository;
    private RideRepository rideRepository;
    private RideService rideService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        rideRepository = mock(RideRepository.class);
        rideService = new RideService(userRepository, rideRepository);
    }

    @Test
    void publishesActiveRideForNormalizedDriver() {
        User driver = driver("driver@example.com");
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ride ride = rideService.publish(" DRIVER@EXAMPLE.COM ", request(" Campus ", " Office "));

        assertEquals(driver, ride.getDriver());
        assertEquals("Campus", ride.getSource());
        assertEquals("Office", ride.getDestination());
        assertEquals(RideStatus.ACTIVE, ride.getStatus());
        verify(rideRepository).save(ride);
    }

    @Test
    void rejectsNonDriver() {
        User rider = new User();
        rider.setEmail("rider@example.com");
        rider.setRoles(Set.of(Role.RIDER));
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));

        assertThrows(RideNotFoundException.class,
                () -> rideService.publish(rider.getEmail(), request("A", "B")));
    }

    @Test
    void rejectsAnotherDriversRide() {
        User driver = driver("driver@example.com");
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(42L, driver.getId())).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class,
                () -> rideService.update(driver.getEmail(), 42L, request("A", "B")));
    }

    @Test
    void rejectsEditingAfterDeparture() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));

        assertThrows(RideNotFoundException.class,
                () -> rideService.update(driver.getEmail(), 1L, request("A", "B")));
    }

    @Test
    void cancellationChangesStatusWithoutDeleting() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));
        when(rideRepository.save(ride)).thenReturn(ride);

        rideService.cancel(driver.getEmail(), 1L);

        assertEquals(RideStatus.CANCELLED, ride.getStatus());
        verify(rideRepository).save(ride);
    }

    @Test
    void startChangesStatusToOngoing() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));
        when(rideRepository.save(ride)).thenReturn(ride);

        rideService.start(driver.getEmail(), 1L);

        assertEquals(RideStatus.ONGOING, ride.getStatus());
        verify(rideRepository).save(ride);
    }

    @Test
    void startRejectsNonActiveRide() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().plusHours(1));
        ride.setStatus(RideStatus.CANCELLED);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));

        assertThrows(RideNotFoundException.class,
                () -> rideService.start(driver.getEmail(), 1L));
    }

    @Test
    void completeChangesStatusToCompleted() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().plusHours(1));
        ride.setStatus(RideStatus.ONGOING);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));
        when(rideRepository.save(ride)).thenReturn(ride);

        rideService.complete(driver.getEmail(), 1L);

        assertEquals(RideStatus.COMPLETED, ride.getStatus());
        verify(rideRepository).save(ride);
    }

    @Test
    void completeRejectsNonOngoingRide() {
        User driver = driver("driver@example.com");
        Ride ride = ride(driver, LocalDateTime.now().plusHours(1));
        ride.setStatus(RideStatus.ACTIVE);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(rideRepository.findByIdAndDriverId(1L, driver.getId())).thenReturn(Optional.of(ride));

        assertThrows(RideNotFoundException.class,
                () -> rideService.complete(driver.getEmail(), 1L));
    }

    private User driver(String email) {
        User driver = new User();
        driver.setId(7L);
        driver.setEmail(email);
        driver.setRoles(Set.of(Role.DRIVER));
        return driver;
    }

    private Ride ride(User driver, LocalDateTime departureTime) {
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setDriver(driver);
        ride.setSource("A");
        ride.setDestination("B");
        ride.setDepartureTime(departureTime);
        ride.setSeats(3);
        ride.setPrice(BigDecimal.ZERO);
        ride.setStatus(RideStatus.ACTIVE);
        return ride;
    }

    private RideRequest request(String source, String destination) {
        RideRequest request = new RideRequest();
        request.setSource(source);
        request.setDestination(destination);
        request.setDepartureTime(LocalDateTime.now().plusHours(2));
        request.setSeats(3);
        request.setPrice(BigDecimal.ZERO);
        return request;
    }
}
