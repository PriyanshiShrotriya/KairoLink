package com.KairoLink.controller;

import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.IssueStatus;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.IssueReportRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final IssueReportRepository issueReportRepository;

    public DashboardController(
            UserRepository userRepository,
            RideRepository rideRepository,
            BookingRepository bookingRepository,
            IssueReportRepository issueReportRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.issueReportRepository = issueReportRepository;
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
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Model model) {
        // --- 1. Existing KPI Metrics (Monday UTC Calendar-Week Semantics) ---
        long totalUsers = userRepository.count();
        long activeRides = rideRepository.countByStatusActive();

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant startOfWeek = todayUtc
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        long bookingsThisWeek = bookingRepository.countBookingsCreatedAfter(startOfWeek);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeRides", activeRides);
        model.addAttribute("bookingsThisWeek", bookingsThisWeek);

        // --- 2. Booking Activity Line Chart (Last 7 Days, UTC-based) ---
        // Grouping and date boundaries are calculated using UTC to ensure consistency across environments.
        Instant sevenDaysAgo = todayUtc.minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfToday = todayUtc.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Instant> createdTimestamps = bookingRepository.findCreatedAtInRange(sevenDaysAgo, endOfToday);

        Map<LocalDate, Long> countsByDate = new HashMap<>();
        for (Instant ts : createdTimestamps) {
            LocalDate d = ts.atZone(ZoneOffset.UTC).toLocalDate();
            countsByDate.merge(d, 1L, Long::sum);
        }

        List<String> bookingActivityLabels = new ArrayList<>();
        List<Long> bookingActivityData = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("EEE (d MMM)", Locale.ENGLISH);

        for (int i = 6; i >= 0; i--) {
            LocalDate date = todayUtc.minusDays(i);
            bookingActivityLabels.add(date.format(labelFormatter));
            bookingActivityData.add(countsByDate.getOrDefault(date, 0L));
        }

        model.addAttribute("bookingActivityLabels", bookingActivityLabels);
        model.addAttribute("bookingActivityData", bookingActivityData);

        // --- 3. Ride Status Distribution ---
        Map<RideStatus, Long> rideStatusMap = new EnumMap<>(RideStatus.class);
        for (RideStatus s : RideStatus.values()) {
            rideStatusMap.put(s, 0L);
        }
        for (Object[] row : rideRepository.countByStatusGrouped()) {
            rideStatusMap.put((RideStatus) row[0], (Long) row[1]);
        }
        List<String> rideStatusLabels = new ArrayList<>();
        List<Long> rideStatusData = new ArrayList<>();
        for (RideStatus s : RideStatus.values()) {
            rideStatusLabels.add(s.name());
            rideStatusData.add(rideStatusMap.get(s));
        }
        model.addAttribute("rideStatusLabels", rideStatusLabels);
        model.addAttribute("rideStatusData", rideStatusData);

        // --- 4. Booking Status Distribution ---
        Map<BookingStatus, Long> bookingStatusMap = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values()) {
            bookingStatusMap.put(s, 0L);
        }
        for (Object[] row : bookingRepository.countByStatus()) {
            bookingStatusMap.put((BookingStatus) row[0], (Long) row[1]);
        }
        List<String> bookingStatusLabels = new ArrayList<>();
        List<Long> bookingStatusData = new ArrayList<>();
        for (BookingStatus s : BookingStatus.values()) {
            bookingStatusLabels.add(s.name());
            bookingStatusData.add(bookingStatusMap.get(s));
        }
        model.addAttribute("bookingStatusLabels", bookingStatusLabels);
        model.addAttribute("bookingStatusData", bookingStatusData);

        // --- 5. Optional Issue Summary ---
        Map<IssueStatus, Long> issueStatusMap = new EnumMap<>(IssueStatus.class);
        for (IssueStatus s : IssueStatus.values()) {
            issueStatusMap.put(s, 0L);
        }
        for (Object[] row : issueReportRepository.countByStatus()) {
            issueStatusMap.put((IssueStatus) row[0], (Long) row[1]);
        }
        model.addAttribute("issuesOpen", issueStatusMap.get(IssueStatus.OPEN));
        model.addAttribute("issuesInProgress", issueStatusMap.get(IssueStatus.IN_PROGRESS));
        model.addAttribute("issuesResolved", issueStatusMap.get(IssueStatus.RESOLVED));
        model.addAttribute("totalIssues", issueStatusMap.values().stream().mapToLong(Long::longValue).sum());

        return "dashboard/admin";
    }
}