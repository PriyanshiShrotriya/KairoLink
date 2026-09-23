package com.KairoLink.controller;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.User;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.service.UserManagementService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserManagementService userManagementService;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public AdminController(
            UserManagementService userManagementService,
            RideRepository rideRepository,
            BookingRepository bookingRepository) {
        this.userManagementService = userManagementService;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userManagementService.getAllUsers();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{userId}/suspend")
    public String suspendUser(
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            userManagementService.suspendUser(userId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "User suspended successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/ban")
    public String banUser(
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            userManagementService.banUser(userId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "User banned successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/reactivate")
    public String reactivateUser(
            @PathVariable Long userId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            userManagementService.reactivateUser(userId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "User reactivated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/rides")
    public String listRides(Model model) {
        List<Ride> rides = rideRepository.findAllRidesWithDriver();
        model.addAttribute("rides", rides);
        return "admin/rides";
    }

    @GetMapping("/bookings")
    public String listBookings(Model model) {
        List<Booking> bookings = bookingRepository.findAllBookingsWithDetails();
        model.addAttribute("bookings", bookings);
        return "admin/bookings";
    }
}

