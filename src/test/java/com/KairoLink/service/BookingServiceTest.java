package com.KairoLink.service;

import com.KairoLink.dto.BookingRequest;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.repository.BookingRepository;
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

class BookingServiceTest {

    private UserRepository userRepository;
    private RideRepository rideRepository;
    private BookingRepository bookingRepository;
    private NotificationService notificationService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        rideRepository = mock(RideRepository.class);
        bookingRepository = mock(BookingRepository.class);
        notificationService = mock(NotificationService.class);
        bookingService = new BookingService(userRepository, rideRepository, bookingRepository, notificationService);
    }

    @Test
    void createsPendingBookingWithoutChangingRideSeats() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        rider.setName("Rider");
        Ride ride = ride(2L, 3);
        BookingRequest request = request(2);
        when(userRepository.findByEmail("rider@example.com")).thenReturn(Optional.of(rider));
        when(rideRepository.findById(7L)).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRiderIdAndRideIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        Booking booking = bookingService.request(" RIDER@EXAMPLE.COM ", 7L, request);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals(new BigDecimal("50.00"), booking.getTotalPrice());
        assertEquals(3, ride.getSeats());
        verify(notificationService).createNotification(
                "driver@example.com",
                NotificationType.BOOKING_REQUESTED,
                "New booking request",
                "Rider requested 2 seat(s) on your ride",
                11L);
    }

    @Test
    void rejectsDuplicateActiveBooking() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Ride ride = ride(2L, 3);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(rideRepository.findById(7L)).thenReturn(Optional.of(ride));
        when(bookingRepository.existsByRiderIdAndRideIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThrows(BookingConflictException.class,
                () -> bookingService.request(rider.getEmail(), 7L, request(1)));
    }

    @Test
    void rejectsOwnRide() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        Ride ride = ride(2L, 3);
        ride.setDriver(driver);
        User rider = user(2L, "driver@example.com", Role.RIDER);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(rideRepository.findById(7L)).thenReturn(Optional.of(ride));

        assertThrows(BookingConflictException.class,
                () -> bookingService.request(rider.getEmail(), 7L, request(1)));
    }

    @Test
    void acceptsPendingBookingAndDecrementsSeatsAtomically() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        Booking booking = booking(driver, BookingStatus.PENDING, 2);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndRideDriverId(11L, driver.getId())).thenReturn(Optional.of(booking));
        when(rideRepository.decrementSeatsIfAvailable(7L, 2)).thenReturn(1);
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking accepted = bookingService.accept(driver.getEmail(), 11L);

        assertEquals(BookingStatus.CONFIRMED, accepted.getStatus());
        verify(rideRepository).decrementSeatsIfAvailable(7L, 2);
    }

    @Test
    void rejectsPendingBookingWithoutChangingSeats() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        Booking booking = booking(driver, BookingStatus.PENDING, 1);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndRideDriverId(11L, driver.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking rejected = bookingService.reject(driver.getEmail(), 11L);

        assertEquals(BookingStatus.REJECTED, rejected.getStatus());
        org.mockito.Mockito.verifyNoInteractions(rideRepository);
    }

    @Test
    void cannotAcceptWhenAtomicSeatUpdateFindsInsufficientSeats() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        Booking booking = booking(driver, BookingStatus.PENDING, 2);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndRideDriverId(11L, driver.getId())).thenReturn(Optional.of(booking));
        when(rideRepository.decrementSeatsIfAvailable(7L, 2)).thenReturn(0);

        assertThrows(BookingConflictException.class,
                () -> bookingService.accept(driver.getEmail(), 11L));
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void cannotProcessBookingTwice() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        Booking booking = booking(driver, BookingStatus.CONFIRMED, 1);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(bookingRepository.findByIdAndRideDriverId(11L, driver.getId())).thenReturn(Optional.of(booking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.accept(driver.getEmail(), 11L));
        org.mockito.Mockito.verifyNoInteractions(rideRepository);
    }

    private BookingRequest request(int seats) {
        BookingRequest request = new BookingRequest();
        request.setSeatsRequested(seats);
        return request;
    }

    private Ride ride(Long driverId, int seats) {
        Ride ride = new Ride();
        User driver = user(driverId, "driver@example.com", Role.DRIVER);
        ride.setId(7L);
        ride.setDriver(driver);
        ride.setSource("Campus");
        ride.setDestination("Office");
        ride.setDepartureTime(LocalDateTime.now().plusDays(1));
        ride.setSeats(seats);
        ride.setPrice(new BigDecimal("25.00"));
        ride.setStatus(RideStatus.ACTIVE);
        return ride;
    }

    private User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRoles(Set.of(role));
        return user;
    }

    private Booking booking(User driver, BookingStatus status, int seatsRequested) {
        Booking booking = new Booking();
        booking.setId(11L);
        booking.setRide(ride(driver.getId(), 3));
        booking.getRide().setDriver(driver);
        booking.setRider(user(1L, "rider@example.com", Role.RIDER));
        booking.setStatus(status);
        booking.setSeatsRequested(seatsRequested);
        booking.setTotalPrice(new BigDecimal("50.00"));
        return booking;
    }
}
