package com.KairoLink.service;

import com.KairoLink.dto.BookingRequest;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.exception.BookingNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
public class BookingService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public BookingService(
            UserRepository userRepository,
            RideRepository rideRepository,
            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Booking request(String email, Long rideId, BookingRequest request) {
        User rider = findRider(email);
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new BookingNotFoundException("Ride was not found"));

        validateRideForBooking(ride, rider, request.getSeatsRequested());

        if (bookingRepository.existsByRiderIdAndRideIdAndStatusIn(
                rider.getId(), ride.getId(), EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))) {
            throw new BookingConflictException("An active booking already exists for this ride");
        }

        Booking booking = new Booking();
        booking.setRider(rider);
        booking.setRide(ride);
        booking.setStatus(BookingStatus.PENDING);
        booking.setSeatsRequested(request.getSeatsRequested());
        booking.setTotalPrice(ride.getPrice().multiply(BigDecimal.valueOf(request.getSeatsRequested())));
        return bookingRepository.save(booking);
    }

    public List<Booking> findDriverBookings(String email) {
        User driver = findDriver(email);
        return bookingRepository.findByRideDriverIdOrderByCreatedAtDesc(driver.getId());
    }

    public List<Booking> findRiderBookings(String email) {
        User rider = findRider(email);
        return bookingRepository.findByRiderIdOrderByCreatedAtDesc(rider.getId());
    }

    @Transactional
    public Booking accept(String email, Long bookingId) {
        User driver = findDriver(email);
        Booking booking = bookingRepository.findByIdAndRideDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking was not found"));
        ensurePending(booking);

        int updatedRows = rideRepository.decrementSeatsIfAvailable(
                booking.getRide().getId(), booking.getSeatsRequested());
        if (updatedRows != 1) {
            throw new BookingConflictException("The ride no longer has enough available seats");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking reject(String email, Long bookingId) {
        User driver = findDriver(email);
        Booking booking = bookingRepository.findByIdAndRideDriverId(bookingId, driver.getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking was not found"));
        ensurePending(booking);
        booking.setStatus(BookingStatus.REJECTED);
        return bookingRepository.save(booking);
    }

    private void validateRideForBooking(Ride ride, User rider, int seatsRequested) {
        if (seatsRequested <= 0) {
            throw new BookingConflictException("At least one seat must be requested");
        }
        if (ride.getDriver().getId().equals(rider.getId())) {
            throw new BookingConflictException("You cannot book your own ride");
        }
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw new BookingConflictException("This ride is no longer accepting requests");
        }
        if (ride.getDepartureTime() == null || !ride.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BookingConflictException("This ride has already departed");
        }
        if (seatsRequested > ride.getSeats()) {
            throw new BookingConflictException("The requested seats are not available");
        }
    }

    private User findRider(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
        if (!user.getRoles().contains(Role.RIDER)) {
            throw new BookingNotFoundException("Rider booking was not found");
        }
        return user;
    }

    private User findDriver(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
        if (!user.getRoles().contains(Role.DRIVER)) {
            throw new BookingNotFoundException("Driver booking was not found");
        }
        return user;
    }

    private void ensurePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Booking has already been processed");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
