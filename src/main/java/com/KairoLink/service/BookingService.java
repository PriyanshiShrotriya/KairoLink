package com.KairoLink.service;

import com.KairoLink.dto.BookingView;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.exception.RideIntegrationUnavailableException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.ride.RideGateway;
import com.KairoLink.ride.RideSummary;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;

@Service
public class BookingService {

    private static final EnumSet<BookingStatus> ACTIVE_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<RideGateway> rideGatewayProvider;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            ObjectProvider<RideGateway> rideGatewayProvider) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.rideGatewayProvider = rideGatewayProvider;
    }

    @Transactional
    public BookingView requestBooking(Long riderId, Long rideId) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new BookingConflictException("Rider account was not found"));
        RideSummary ride = requireGateway().findBookableRide(rideId);
        if (ride == null) {
            throw new BookingConflictException("Ride is not available for booking");
        }
        if (riderId.equals(ride.driverId())) {
            throw new BookingConflictException("A driver cannot book their own ride");
        }
        if (bookingRepository.existsByRiderIdAndRideIdAndStatusIn(
                riderId, rideId, ACTIVE_STATUSES)) {
            throw new BookingConflictException("You already have an active booking for this ride");
        }

        Booking booking = new Booking();
        booking.setRider(rider);
        booking.setRideId(rideId);
        booking.setActiveBookingKey(rideId);
        booking.setStatus(BookingStatus.PENDING);
        try {
            return toView(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException exception) {
            if (isActiveBookingConstraintViolation(exception)) {
                throw new BookingConflictException(
                        "You already have an active booking for this ride", exception);
            }
            throw exception;
        }
    }

    @Transactional
    public BookingView acceptBooking(Long driverId, Long bookingId) {
        Booking booking = findBooking(bookingId);
        ensurePending(booking);
        RideSummary ride = requireGateway().findBookableRide(booking.getRideId());
        ensureDriver(ride, driverId);
        if (!requireGateway().confirmSeat(booking.getRideId())) {
            throw new BookingConflictException("No seat is available for this ride");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return toView(bookingRepository.saveAndFlush(booking));
    }

    @Transactional
    public BookingView rejectBooking(Long driverId, Long bookingId) {
        Booking booking = findBooking(bookingId);
        ensurePending(booking);
        ensureDriver(requireGateway().findBookableRide(booking.getRideId()), driverId);
        booking.setStatus(BookingStatus.REJECTED);
        booking.setActiveBookingKey(null);
        return toView(bookingRepository.saveAndFlush(booking));
    }

    @Transactional
    public BookingView cancelBooking(Long riderId, Long bookingId) {
        Booking booking = bookingRepository.findByIdAndRiderId(bookingId, riderId)
                .orElseThrow(() -> new BookingConflictException("Booking was not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Only pending bookings can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setActiveBookingKey(null);
        return toView(bookingRepository.saveAndFlush(booking));
    }

    @Transactional
    public BookingView getBooking(Long riderId, Long bookingId) {
        return toView(bookingRepository.findByIdAndRiderId(bookingId, riderId)
                .orElseThrow(() -> new BookingConflictException("Booking was not found")));
    }

    @Transactional
    public List<BookingView> listBookings(Long riderId) {
        return bookingRepository.findAllByRiderIdOrderByRequestedAtDesc(riderId)
                .stream()
                .map(this::toView)
                .toList();
    }

    private Booking findBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingConflictException("Booking was not found"));
    }

    private void ensurePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Only pending bookings can be updated");
        }
    }

    private void ensureDriver(RideSummary ride, Long driverId) {
        if (ride == null || !driverId.equals(ride.driverId())) {
            throw new BookingConflictException("You are not the driver of this ride");
        }
    }

    private RideGateway requireGateway() {
        RideGateway gateway = rideGatewayProvider.getIfAvailable();
        if (gateway == null) {
            throw new RideIntegrationUnavailableException();
        }
        return gateway;
    }

    private BookingView toView(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getRideId(),
                booking.getStatus(),
                booking.getRequestedAt(),
                booking.getUpdatedAt());
    }

    private boolean isActiveBookingConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains("uk_bookings_active_rider_ride")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
