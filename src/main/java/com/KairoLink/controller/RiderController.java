package com.KairoLink.controller;

import com.KairoLink.dto.BookingRequest;
import com.KairoLink.dto.RiderSearchRequest;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.Ride;
import com.KairoLink.service.BookingService;
import com.KairoLink.service.RideService;
import com.KairoLink.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class RiderController {

    private final RideService rideService;
    private final BookingService bookingService;
    private final VehicleService vehicleService;

    public RiderController(
            RideService rideService,
            BookingService bookingService,
            VehicleService vehicleService) {
        this.rideService = rideService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/rider/search")
    public String searchPage() {
        return "rider/search";
    }

    @GetMapping("/rider/results")
    public String results(
            @Valid @ModelAttribute("searchRequest") RiderSearchRequest searchRequest,
            BindingResult bindingResult,
            Model model) {
        String normalizedSource = normalizeOptional(searchRequest.getSource());
        String normalizedDestination = normalizeOptional(searchRequest.getDestination());
        LocalDate effectiveDate = (searchRequest.getDate() != null
                || normalizedSource == null || normalizedDestination == null)
                ? searchRequest.getDate()
                : LocalDate.now();
        model.addAttribute("source", normalizedSource);
        model.addAttribute("destination", normalizedDestination);
        model.addAttribute("date", effectiveDate);
        model.addAttribute("preferredDepartureTime", searchRequest.getTime());
        model.addAttribute("timeToleranceMinutes", searchRequest.getTimeToleranceMinutes());
        model.addAttribute("rides", bindingResult.hasErrors()
                ? java.util.List.of()
                : rideService.search(searchRequest.getSource(), searchRequest.getDestination(), effectiveDate));
        return "rider/results";
    }

    @GetMapping("/rider/ride/{id}")
    public String details(@PathVariable Long id, Model model) {
        Ride ride = rideService.getRide(id);
        addRideDetails(model, ride);
        return "rider/details";
    }

    @GetMapping("/rider/ride/{id}/request")
    public String requestPage(@PathVariable Long id, Model model) {
        Ride ride = rideService.getRide(id);
        addRideDetails(model, ride);
        model.addAttribute("bookingRequest", new BookingRequest());
        return "rider/booking-request";
    }

    @PostMapping("/rider/ride/{id}/request")
    public String request(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("bookingRequest") BookingRequest request,
            BindingResult bindingResult,
            Model model) {
        Ride ride = rideService.getRide(id);
        addRideDetails(model, ride);
        if (bindingResult.hasErrors()) {
            return "rider/booking-request";
        }

        Booking booking = bookingService.request(authentication.getName(), id, request);
        model.addAttribute("booking", booking);
        model.addAttribute("totalPrice", booking.getTotalPrice());
        return "rider/booking-request";
    }

    @GetMapping("/rider/bookings")
    public String bookings(Authentication authentication, Model model) {
        model.addAttribute("bookings", bookingService.findRiderBookings(authentication.getName()));
        return "rider/my-bookings";
    }

    private void addRideDetails(Model model, Ride ride) {
        model.addAttribute("ride", ride);
        model.addAttribute("driver", ride.getDriver());
        model.addAttribute("vehicle", vehicleService.getVehicle(ride.getDriver().getEmail()).orElse(null));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
