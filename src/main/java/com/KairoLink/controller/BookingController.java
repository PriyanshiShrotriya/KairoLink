package com.KairoLink.controller;

import com.KairoLink.service.BookingService;
import com.KairoLink.dto.RoundTripDecisionForm;
import com.KairoLink.service.RoundTripRequestService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final RoundTripRequestService roundTripRequestService;

    public BookingController(BookingService bookingService, RoundTripRequestService roundTripRequestService) {
        this.bookingService = bookingService;
        this.roundTripRequestService = roundTripRequestService;
    }

    @GetMapping("/driver/bookings")
    public String driverBookings(
            Authentication authentication,
            @RequestParam(required = false) Long booking,
            org.springframework.ui.Model model) {
        model.addAttribute("bookings", bookingService.findDriverBookings(authentication.getName()));
        model.addAttribute("roundTripRequests", roundTripRequestService.findForDriver(authentication.getName()));
        model.addAttribute("selectedBookingId", booking);
        return "driver/bookings";
    }

    @PostMapping("/driver/bookings/{id}/accept")
    public String accept(Authentication authentication, @PathVariable Long id) {
        bookingService.accept(authentication.getName(), id);
        return "redirect:/driver/bookings?accepted";
    }

    @PostMapping("/driver/bookings/{id}/reject")
    public String reject(Authentication authentication, @PathVariable Long id) {
        bookingService.reject(authentication.getName(), id);
        return "redirect:/driver/bookings?rejected";
    }

    @PostMapping("/driver/round-trip-requests/{id}/accept")
    public String acceptRoundTrip(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.ModelAttribute("roundTripDecision")
                    RoundTripDecisionForm form,
            BindingResult bindingResult,
            org.springframework.ui.Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bookings", bookingService.findDriverBookings(authentication.getName()));
            model.addAttribute("roundTripRequests", roundTripRequestService.findForDriver(authentication.getName()));
            return "driver/bookings";
        }
        roundTripRequestService.accept(authentication.getName(), id, form);
        return "redirect:/driver/bookings?roundTripAccepted";
    }

    @PostMapping("/driver/round-trip-requests/{id}/reject")
    public String rejectRoundTrip(Authentication authentication, @PathVariable Long id) {
        roundTripRequestService.reject(authentication.getName(), id);
        return "redirect:/driver/bookings?roundTripRejected";
    }
}
