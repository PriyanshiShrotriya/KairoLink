package com.KairoLink.service;

import com.KairoLink.dto.RoundTripDecisionForm;
import com.KairoLink.dto.RoundTripRequestForm;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.RoundTripRequest;
import com.KairoLink.entity.RoundTripRequestStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.BookingConflictException;
import com.KairoLink.exception.RoundTripRequestNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.RoundTripRequestRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

@Service
public class RoundTripRequestService {

    private static final DateTimeFormatter NOTIFICATION_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mm a");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final RoundTripRequestRepository requestRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Autowired
    public RoundTripRequestService(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            RideRepository rideRepository,
            RoundTripRequestRepository requestRepository,
            NotificationService notificationService) {
        this(userRepository, bookingRepository, rideRepository, requestRepository,
                notificationService, Clock.systemDefaultZone());
    }

    RoundTripRequestService(
            UserRepository userRepository,
            BookingRepository bookingRepository,
            RideRepository rideRepository,
            RoundTripRequestRepository requestRepository,
            NotificationService notificationService,
            Clock clock) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    public RoundTripRequest create(String email, Long bookingId, RoundTripRequestForm form) {
        User rider = findUser(email, Role.RIDER);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RoundTripRequestNotFoundException("Outbound booking was not found"));
        if (!booking.getRider().getId().equals(rider.getId())) {
            throw new RoundTripRequestNotFoundException("Outbound booking was not found");
        }
        validateEligibleBooking(booking);
        validateReturnForm(form);
        validateSameDayReturn(booking, form.getRequestedReturnDate());
        LocalDateTime requestedAt = LocalDateTime.of(
                form.getRequestedReturnDate(), form.getRequestedReturnTime());
        validateReturnTime(requestedAt);
        if (requestRepository.existsByOutboundBookingIdAndStatusIn(
                bookingId, EnumSet.of(RoundTripRequestStatus.PENDING, RoundTripRequestStatus.ACCEPTED))) {
            throw new BookingConflictException("An active round-trip request already exists for this booking");
        }

        RoundTripRequest request = new RoundTripRequest();
        request.setOutboundBooking(booking);
        request.setRequestedReturnDate(form.getRequestedReturnDate());
        request.setRequestedReturnTime(form.getRequestedReturnTime());
        request.setReturnPickup(normalize(form.getReturnPickup(), "Return pickup is required"));
        request.setReturnDestination(normalize(form.getReturnDestination(), "Return destination is required"));
        request.setPassengerMessage(normalizeOptional(form.getPassengerMessage()));
        request.setStatus(RoundTripRequestStatus.PENDING);
        RoundTripRequest saved = requestRepository.save(request);
        notificationService.createNotification(
                booking.getRide().getDriver().getEmail(),
                NotificationType.GENERAL,
                "Round-trip request",
                rider.getName() + " requested a return journey. It is awaiting your response.",
                booking.getId());
        return saved;
    }

    public List<RoundTripRequest> findForRider(String email) {
        return requestRepository.findByRiderIdOrderByCreatedAtDesc(findUser(email, Role.RIDER).getId());
    }

    public List<RoundTripRequest> findForDriver(String email) {
        return requestRepository.findByDriverIdOrderByCreatedAtDesc(findUser(email, Role.DRIVER).getId());
    }

    public RoundTripRequest getForRider(String email, Long requestId) {
        User rider = findUser(email, Role.RIDER);
        return requestRepository.findByIdAndOutboundBookingRiderId(requestId, rider.getId())
                .orElseThrow(() -> new RoundTripRequestNotFoundException("Round-trip request was not found"));
    }

    public boolean canRequest(String email, Booking booking) {
        if (booking == null) {
            return false;
        }
        try {
            User rider = findUser(email, Role.RIDER);
            if (booking.getRider() == null || !rider.getId().equals(booking.getRider().getId())) {
                return false;
            }
            validateEligibleBooking(booking);
            return !requestRepository.existsByOutboundBookingIdAndStatusIn(
                    booking.getId(), EnumSet.of(
                            RoundTripRequestStatus.PENDING, RoundTripRequestStatus.ACCEPTED));
        } catch (BookingConflictException | UserNotFoundException exception) {
            return false;
        }
    }

    @Transactional
    public RoundTripRequest accept(String email, Long requestId, RoundTripDecisionForm form) {
        User driver = findUser(email, Role.DRIVER);
        RoundTripRequest request = requestRepository.findForUpdate(requestId)
                .orElseThrow(() -> new RoundTripRequestNotFoundException("Round-trip request was not found"));
        if (!request.getOutboundBooking().getRide().getDriver().getId().equals(driver.getId())) {
            throw new RoundTripRequestNotFoundException("Round-trip request was not found");
        }
        if (request.getStatus() != RoundTripRequestStatus.PENDING) {
            throw new BookingConflictException("Round-trip request has already been processed");
        }
        LocalDateTime departure = LocalDateTime.of(
                form.getConfirmedReturnDate(), form.getConfirmedReturnTime());
        validateReturnTime(departure);
        Booking outbound = request.getOutboundBooking();
        if (outbound.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingConflictException("The outbound booking is no longer confirmed");
        }
        validateSameDayReturn(outbound, form.getConfirmedReturnDate());
        if (rideRepository.existsByDriverIdAndDepartureTimeAndStatusIn(
                driver.getId(), departure, EnumSet.of(RideStatus.CREATED, RideStatus.ACTIVE, RideStatus.ONGOING))) {
            throw new BookingConflictException("The confirmed return time conflicts with another ride");
        }

        Ride outboundRide = outbound.getRide();
        Ride returnRide = new Ride();
        returnRide.setDriver(driver);
        returnRide.setSource(request.getReturnPickup());
        returnRide.setDestination(request.getReturnDestination());
        returnRide.setDepartureTime(departure);
        returnRide.setSeats(outbound.getSeatsRequested());
        returnRide.setPrice(outboundRide.getPrice());
        returnRide.setStatus(RideStatus.ACTIVE);
        Ride savedRide = rideRepository.save(returnRide);

        Booking returnBooking = new Booking();
        returnBooking.setRider(outbound.getRider());
        returnBooking.setRide(savedRide);
        returnBooking.setStatus(BookingStatus.CONFIRMED);
        returnBooking.setSeatsRequested(outbound.getSeatsRequested());
        returnBooking.setTotalPrice(savedRide.getPrice().multiply(
                BigDecimal.valueOf(outbound.getSeatsRequested())));
        bookingRepository.save(returnBooking);

        request.setConfirmedReturnRide(savedRide);
        request.setDriverResponse("Return journey confirmed");
        request.setStatus(RoundTripRequestStatus.ACCEPTED);
        RoundTripRequest saved = requestRepository.save(request);
        notificationService.createNotification(
                outbound.getRider().getEmail(),
                NotificationType.GENERAL,
                "Round-trip accepted",
                acceptedMessage(departure),
                outbound.getId());
        return saved;
    }

    @Transactional
    public RoundTripRequest reject(String email, Long requestId) {
        User driver = findUser(email, Role.DRIVER);
        RoundTripRequest request = requestRepository.findForUpdate(requestId)
                .orElseThrow(() -> new RoundTripRequestNotFoundException("Round-trip request was not found"));
        if (!request.getOutboundBooking().getRide().getDriver().getId().equals(driver.getId())) {
            throw new RoundTripRequestNotFoundException("Round-trip request was not found");
        }
        if (request.getStatus() != RoundTripRequestStatus.PENDING) {
            throw new BookingConflictException("Round-trip request has already been processed");
        }
        request.setStatus(RoundTripRequestStatus.REJECTED);
        request.setDriverResponse("Return journey declined");
        RoundTripRequest saved = requestRepository.save(request);
        notificationService.createNotification(
                request.getOutboundBooking().getRider().getEmail(),
                NotificationType.GENERAL,
                "Round-trip rejected",
                "Your driver has declined your return journey request.",
                request.getOutboundBooking().getId());
        return saved;
    }

    @Transactional
    public RoundTripRequest cancel(String email, Long requestId) {
        RoundTripRequest request = getForRider(email, requestId);
        if (request.getStatus() != RoundTripRequestStatus.PENDING) {
            throw new BookingConflictException("Only pending round-trip requests can be cancelled");
        }
        request.setStatus(RoundTripRequestStatus.CANCELLED);
        return requestRepository.save(request);
    }

    private void validateEligibleBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingConflictException("A confirmed outbound booking is required");
        }

        if (booking.getRide() == null || booking.getRide().getDepartureTime() == null) {
            throw new BookingConflictException("The outbound ride has no departure time");
        }
        if (!booking.getRide().getDepartureTime().toLocalDate()
                .equals(LocalDate.now(clock))) {
            throw new BookingConflictException(
                    "A return request can only be made on the outbound ride's calendar date");
        }
    }

    private String acceptedMessage(LocalDateTime departure) {
        if (departure == null) {
            return "Your return journey has been accepted.";
        }
        return "Your return journey has been accepted for "
                + NOTIFICATION_DATE_TIME_FORMAT.format(departure) + ".";
    }

    private void validateReturnTime(LocalDateTime dateTime) {
        if (dateTime == null || !dateTime.isAfter(LocalDateTime.now(clock))) {
            throw new BookingConflictException("The return departure must be in the future");
        }
    }

    private void validateSameDayReturn(Booking booking, LocalDate returnDate) {
        if (returnDate == null || booking.getRide() == null
                || !returnDate.equals(booking.getRide().getDepartureTime().toLocalDate())) {
            throw new BookingConflictException(
                    "The return journey must be scheduled on the outbound ride's calendar date");
        }
    }

    private void validateReturnForm(RoundTripRequestForm form) {
        if (form == null
                || form.getRequestedReturnDate() == null
                || form.getRequestedReturnTime() == null) {
            throw new BookingConflictException("A return date and time are required");
        }
        if (form.getReturnPickup() == null || form.getReturnDestination() == null) {
            throw new BookingConflictException("Return pickup and destination are required");
        }
    }

    private User findUser(String email, Role role) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
        if (!user.getRoles().contains(role)) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return user;
    }

    private String normalize(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BookingConflictException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
