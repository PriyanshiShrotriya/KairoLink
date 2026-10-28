package com.KairoLink.service;

import com.KairoLink.dto.RoundTripDecisionForm;
import com.KairoLink.dto.RoundTripRequestForm;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.RoundTripRequest;
import com.KairoLink.entity.RoundTripRequestStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.exception.RoundTripRequestNotFoundException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.RoundTripRequestRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class RoundTripRequestServiceTest {

    private UserRepository userRepository;
    private BookingRepository bookingRepository;
    private RideRepository rideRepository;
    private RoundTripRequestRepository requestRepository;
    private NotificationService notificationService;
    private RoundTripRequestService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        rideRepository = mock(RideRepository.class);
        requestRepository = mock(RoundTripRequestRepository.class);
        notificationService = mock(NotificationService.class);
        service = new RoundTripRequestService(
                userRepository, bookingRepository, rideRepository, requestRepository, notificationService);
    }

    @Test
    void createsRequestForOwningConfirmedPassenger() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.CONFIRMED);
        RoundTripRequestForm form = form();
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));
        when(requestRepository.existsByOutboundBookingIdAndStatusIn(any(), any())).thenReturn(false);
        when(requestRepository.save(any(RoundTripRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoundTripRequest saved = service.create(rider.getEmail(), 7L, form);

        assertEquals(RoundTripRequestStatus.PENDING, saved.getStatus());
        assertEquals("Office", saved.getReturnPickup());
        verify(notificationService).createNotification(
                "driver@example.com",
                com.KairoLink.entity.NotificationType.GENERAL,
                "Round-trip request",
                "User requested a return journey. It is awaiting your response.",
                7L);
    }

    @Test
    void rejectsRequestForAnotherPassenger() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        User other = user(2L, "other@example.com", Role.RIDER);
        Booking outbound = booking(other, BookingStatus.CONFIRMED);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));

        assertThrows(RoundTripRequestNotFoundException.class,
                () -> service.create(rider.getEmail(), 7L, form()));
    }

    @Test
    void rejectsDuplicateActiveRequest() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.CONFIRMED);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));
        when(requestRepository.existsByOutboundBookingIdAndStatusIn(
                7L, EnumSet.of(RoundTripRequestStatus.PENDING, RoundTripRequestStatus.ACCEPTED)))
                .thenReturn(true);

        assertThrows(BookingConflictException.class,
                () -> service.create(rider.getEmail(), 7L, form()));
    }

    @Test
    void allowsRequestAfterOutboundDepartureOnTheSameDay() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.CONFIRMED);
        outbound.getRide().setDepartureTime(LocalDate.now().atStartOfDay());
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));
        when(requestRepository.existsByOutboundBookingIdAndStatusIn(any(), any())).thenReturn(false);
        when(requestRepository.save(any(RoundTripRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(RoundTripRequestStatus.PENDING,
                service.create(rider.getEmail(), 7L, form()).getStatus());
    }

    @Test
    void rejectsRequestOnADifferentCalendarDate() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.CONFIRMED);
        RoundTripRequestForm request = form();
        request.setRequestedReturnDate(LocalDate.now().plusDays(1));
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));

        assertThrows(BookingConflictException.class,
                () -> service.create(rider.getEmail(), 7L, request));
    }

    @Test
    void rejectsReturnDepartureInThePast() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.CONFIRMED);
        RoundTripRequestForm request = form();
        request.setRequestedReturnTime(LocalTime.MIDNIGHT);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));

        assertThrows(BookingConflictException.class,
                () -> service.create(rider.getEmail(), 7L, request));
    }

    @Test
    void rejectsUnconfirmedOutboundBooking() {
        User rider = user(1L, "rider@example.com", Role.RIDER);
        Booking outbound = booking(rider, BookingStatus.PENDING);
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(outbound));

        assertThrows(BookingConflictException.class,
                () -> service.create(rider.getEmail(), 7L, form()));
    }

    @Test
    void acceptsRequestAndCreatesLinkedReturnBooking() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        User rider = user(1L, "rider@example.com", Role.RIDER);
        RoundTripRequest request = request(rider, driver);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(requestRepository.findForUpdate(11L)).thenReturn(Optional.of(request));
        when(rideRepository.existsByDriverIdAndDepartureTimeAndStatusIn(
                any(), any(), any())).thenReturn(false);
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride saved = invocation.getArgument(0);
            saved.setId(19L);
            return saved;
        });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(request)).thenReturn(request);

        RoundTripRequest accepted = service.accept(
                driver.getEmail(), 11L,
                futureReturn());

        assertEquals(RoundTripRequestStatus.ACCEPTED, accepted.getStatus());
        assertEquals(19L, accepted.getConfirmedReturnRide().getId());
        verify(bookingRepository).save(any(Booking.class));
        verify(notificationService).createNotification(
                "rider@example.com",
                com.KairoLink.entity.NotificationType.GENERAL,
                "Round-trip accepted",
                "Your return journey has been accepted for "
                        + java.time.format.DateTimeFormatter
                        .ofPattern("d MMMM yyyy 'at' h:mm a")
                        .format(accepted.getConfirmedReturnRide().getDepartureTime()) + ".",
                7L);
    }

    @Test
    void cannotAcceptProcessedRequest() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        RoundTripRequest request = request(user(1L, "rider@example.com", Role.RIDER), driver);
        request.setStatus(RoundTripRequestStatus.ACCEPTED);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(requestRepository.findForUpdate(11L)).thenReturn(Optional.of(request));

        assertThrows(BookingConflictException.class,
                () -> service.accept(driver.getEmail(), 11L,
                        futureReturn()));
    }

    @Test
    void rejectsPendingRequestForAnotherDriver() {
        User actualDriver = user(3L, "other-driver@example.com", Role.DRIVER);
        User requestDriver = user(2L, "driver@example.com", Role.DRIVER);
        RoundTripRequest request = request(user(1L, "rider@example.com", Role.RIDER), requestDriver);
        when(userRepository.findByEmail(actualDriver.getEmail())).thenReturn(Optional.of(actualDriver));
        when(requestRepository.findForUpdate(11L)).thenReturn(Optional.of(request));

        assertThrows(RoundTripRequestNotFoundException.class,
                () -> service.accept(actualDriver.getEmail(), 11L, futureReturn()));
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    void rejectsPendingRequestAndNotifiesRider() {
        User driver = user(2L, "driver@example.com", Role.DRIVER);
        User rider = user(1L, "rider@example.com", Role.RIDER);
        RoundTripRequest request = request(rider, driver);
        when(userRepository.findByEmail(driver.getEmail())).thenReturn(Optional.of(driver));
        when(requestRepository.findForUpdate(11L)).thenReturn(Optional.of(request));
        when(requestRepository.save(request)).thenReturn(request);

        RoundTripRequest rejected = service.reject(driver.getEmail(), 11L);

        assertEquals(RoundTripRequestStatus.REJECTED, rejected.getStatus());
        verify(notificationService).createNotification(
                "rider@example.com",
                com.KairoLink.entity.NotificationType.GENERAL,
                "Round-trip rejected",
                "Your driver has declined your return journey request.",
                7L);
    }

    private RoundTripRequestForm form() {
        RoundTripRequestForm form = new RoundTripRequestForm();
        form.setReturnPickup("Office");
        form.setReturnDestination("Campus");
        LocalDateTime requested = LocalDateTime.now().plusMinutes(30);
        form.setRequestedReturnDate(requested.toLocalDate());
        form.setRequestedReturnTime(requested.toLocalTime());
        form.setPassengerMessage("Please let me know when approved");
        return form;
    }

    private RoundTripDecisionForm futureReturn() {
        LocalDateTime departure = LocalDateTime.now().plusMinutes(30);
        return new RoundTripDecisionForm(departure.toLocalDate(), departure.toLocalTime());
    }

    private RoundTripRequest request(User rider, User driver) {
        RoundTripRequest request = new RoundTripRequest();
        request.setId(11L);
        request.setStatus(RoundTripRequestStatus.PENDING);
        request.setOutboundBooking(booking(rider, BookingStatus.CONFIRMED));
        request.getOutboundBooking().getRide().setDriver(driver);
        request.setRequestedReturnDate(LocalDate.now());
        request.setRequestedReturnTime(LocalTime.of(18, 0));
        request.setReturnPickup("Office");
        request.setReturnDestination("Campus");
        return request;
    }

    private Booking booking(User rider, BookingStatus status) {
        Ride ride = new Ride();
        ride.setId(7L);
        ride.setDriver(user(2L, "driver@example.com", Role.DRIVER));
        ride.setSource("Campus");
        ride.setDestination("Office");
        ride.setDepartureTime(LocalDateTime.now().plusHours(1));
        ride.setSeats(2);
        ride.setPrice(new BigDecimal("25.00"));
        ride.setStatus(RideStatus.ACTIVE);

        Booking booking = new Booking();
        booking.setId(7L);
        booking.setRider(rider);
        booking.setRide(ride);
        booking.setStatus(status);
        booking.setSeatsRequested(1);
        booking.setTotalPrice(new BigDecimal("25.00"));
        return booking;
    }

    private User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(role == Role.DRIVER ? "Driver" : "User");
        user.setRoles(Set.of(role));
        return user;
    }
}
