package com.KairoLink.controller;

import com.KairoLink.dto.BookingRequest;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.service.BookingService;
import com.KairoLink.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public String requestBooking(
            @Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
            BindingResult bindingResult,
            Principal principal,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("bookingError", "A valid ride is required");
            return "redirect:/rider/search";
        }
        bookingService.requestBooking(requireRole(principal, Role.RIDER), bookingRequest.getRideId());
        return "redirect:/bookings?requested";
    }

    @GetMapping
    public String myBookings(Principal principal, Model model) {
        model.addAttribute("bookings", bookingService.listBookings(requireRole(principal, Role.RIDER)));
        return "rider/my-bookings";
    }

    @GetMapping("/{bookingId}")
    public String bookingDetails(
            @PathVariable Long bookingId,
            Principal principal,
            Model model) {
        model.addAttribute("booking",
                bookingService.getBooking(requireRole(principal, Role.RIDER), bookingId));
        return "rider/booking-details";
    }

    @PostMapping("/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId, Principal principal) {
        bookingService.cancelBooking(requireRole(principal, Role.RIDER), bookingId);
        return "redirect:/bookings";
    }

    @PostMapping("/{bookingId}/accept")
    public String acceptBooking(@PathVariable Long bookingId, Principal principal) {
        bookingService.acceptBooking(requireRole(principal, Role.DRIVER), bookingId);
        return "redirect:/?bookingUpdated";
    }

    @PostMapping("/{bookingId}/reject")
    public String rejectBooking(@PathVariable Long bookingId, Principal principal) {
        bookingService.rejectBooking(requireRole(principal, Role.DRIVER), bookingId);
        return "redirect:/?bookingUpdated";
    }

    private Long requireRole(Principal principal, Role requiredRole) {
        User user = currentUser(principal);
        if (!user.getRoles().contains(requiredRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This action is not available for your role");
        }
        return user.getId();
    }

    private User currentUser(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user was not found"));
    }
}
