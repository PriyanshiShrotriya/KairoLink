package com.KairoLink.controller;

import com.KairoLink.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/driver/bookings")
    public String driverBookings(Authentication authentication, org.springframework.ui.Model model) {
        model.addAttribute("bookings", bookingService.findDriverBookings(authentication.getName()));
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
}
