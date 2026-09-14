package com.KairoLink.service;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.ride.RideGateway;
import com.KairoLink.ride.RideSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectProvider<RideGateway> rideGatewayProvider;

    @Mock
    private RideGateway rideGateway;

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, userRepository, rideGatewayProvider);
    }

    @Test
    void createsPendingBookingForBookableRide() {
        when(rideGatewayProvider.getIfAvailable()).thenReturn(rideGateway);
        User rider = user(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(rider));
        when(rideGateway.findBookableRide(44L)).thenReturn(ride(44L, 20L));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(7L);
            return booking;
        });

        var result = service.requestBooking(10L, 44L);

        assertEquals(7L, result.id());
        assertEquals(44L, result.rideId());
        assertEquals(BookingStatus.PENDING, result.status());
        verify(bookingRepository).existsByRiderIdAndRideIdAndStatusIn(
                10L, 44L, EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
    }

    @Test
    void rejectsDuplicateActiveBooking() {
        when(rideGatewayProvider.getIfAvailable()).thenReturn(rideGateway);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user(10L)));
        when(rideGateway.findBookableRide(44L)).thenReturn(ride(44L, 20L));
        when(bookingRepository.existsByRiderIdAndRideIdAndStatusIn(
                eq(10L), eq(44L), any())).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> service.requestBooking(10L, 44L));

        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void onlyRideDriverCanAcceptBookingAndSeatMustBeConfirmedByGateway() {
        when(rideGatewayProvider.getIfAvailable()).thenReturn(rideGateway);
        Booking booking = booking(7L, 44L, 10L, BookingStatus.PENDING);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(rideGateway.findBookableRide(44L)).thenReturn(ride(44L, 20L));
        when(rideGateway.confirmSeat(44L)).thenReturn(true);
        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        var result = service.acceptBooking(20L, 7L);

        assertEquals(BookingStatus.CONFIRMED, result.status());
        verify(rideGateway).confirmSeat(44L);
    }

    @Test
    void doesNotConfirmWhenGatewayRejectsSeat() {
        when(rideGatewayProvider.getIfAvailable()).thenReturn(rideGateway);
        Booking booking = booking(7L, 44L, 10L, BookingStatus.PENDING);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(rideGateway.findBookableRide(44L)).thenReturn(ride(44L, 20L));
        when(rideGateway.confirmSeat(44L)).thenReturn(false);

        assertThrows(BookingConflictException.class, () -> service.acceptBooking(20L, 7L));

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void riderCannotReadAnotherRidersBooking() {
        when(bookingRepository.findByIdAndRiderId(7L, 99L)).thenReturn(Optional.empty());

        assertThrows(BookingConflictException.class, () -> service.getBooking(99L, 7L));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Booking booking(Long id, Long rideId, Long riderId, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setRideId(rideId);
        booking.setRider(user(riderId));
        booking.setStatus(status);
        booking.setRequestedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    private RideSummary ride(Long rideId, Long driverId) {
        return new RideSummary(
                rideId, driverId, "Driver", "A", "B", Instant.now(),
                3, 2, BigDecimal.TEN, "ACTIVE", null);
    }
}
