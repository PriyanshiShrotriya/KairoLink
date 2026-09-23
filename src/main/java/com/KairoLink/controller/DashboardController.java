package com.KairoLink.controller;

import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public DashboardController(
            UserRepository userRepository,
            RideRepository rideRepository,
            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/admin";
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DRIVER".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/driver";
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RIDER".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/rider";
        }
        throw new AccessDeniedException("No dashboard role is assigned");
    }

    @GetMapping("/dashboard/rider")
    public String riderDashboard() {
        return "dashboard/rider";
    }

    @GetMapping("/dashboard/driver")
    public String driverDashboard() {
        return "dashboard/driver";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(Model model) {
        long totalUsers = userRepository.count();
        long activeRides = rideRepository.countByStatusActive();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        long bookingsThisWeek = bookingRepository.countBookingsCreatedAfter(startOfWeek);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeRides", activeRides);
        model.addAttribute("bookingsThisWeek", bookingsThisWeek);

        return "dashboard/admin";
    }
}
