package com.KairoLink.service;

import com.KairoLink.dto.RiderSearchRequest;
import com.KairoLink.entity.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class TimeMatchingService {

    private final int driverFlexibilityMinutes;
    private final int riderDefaultToleranceMinutes;
    private final int minimumLeadTimeMinutes;
    private final int defaultSearchWindowMinutes;
    private final int maximumRiderToleranceMinutes;
    private final Clock clock;

    @Autowired
    public TimeMatchingService(
            @Value("${kairolink.matching.driver-flexibility-minutes:30}") int driverFlexibilityMinutes,
            @Value("${kairolink.matching.rider-default-tolerance-minutes:30}") int riderDefaultToleranceMinutes,
            @Value("${kairolink.matching.minimum-lead-time-minutes:10}") int minimumLeadTimeMinutes,
            @Value("${kairolink.matching.default-search-window-minutes:30}") int defaultSearchWindowMinutes,
            @Value("${kairolink.matching.maximum-rider-tolerance-minutes:180}") int maximumRiderToleranceMinutes) {
        this(driverFlexibilityMinutes, riderDefaultToleranceMinutes, minimumLeadTimeMinutes,
                defaultSearchWindowMinutes, maximumRiderToleranceMinutes, Clock.systemDefaultZone());
    }

    TimeMatchingService(
            int driverFlexibilityMinutes,
            int riderDefaultToleranceMinutes,
            int minimumLeadTimeMinutes,
            int defaultSearchWindowMinutes,
            int maximumRiderToleranceMinutes,
            Clock clock) {
        if (driverFlexibilityMinutes < 0 || riderDefaultToleranceMinutes < 0
                || minimumLeadTimeMinutes < 0 || defaultSearchWindowMinutes < 0
                || maximumRiderToleranceMinutes < 0
                || riderDefaultToleranceMinutes > maximumRiderToleranceMinutes) {
            throw new IllegalArgumentException("Time matching configuration is invalid");
        }
        this.driverFlexibilityMinutes = driverFlexibilityMinutes;
        this.riderDefaultToleranceMinutes = riderDefaultToleranceMinutes;
        this.minimumLeadTimeMinutes = minimumLeadTimeMinutes;
        this.defaultSearchWindowMinutes = defaultSearchWindowMinutes;
        this.maximumRiderToleranceMinutes = maximumRiderToleranceMinutes;
        this.clock = clock;
    }

    public boolean matches(Ride ride, RiderSearchRequest request, LocalDate searchDate) {
        if (ride == null || ride.getDepartureTime() == null || request == null) {
            return false;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate requestedDate = searchDate == null ? today : searchDate;
        LocalDateTime driverStart = ride.getDepartureTime()
                .minusMinutes(driverFlexibilityMinutes);
        LocalDateTime driverEnd = ride.getDepartureTime()
                .plusMinutes(driverFlexibilityMinutes);

        LocalDateTime riderStart;
        LocalDateTime riderEnd;
        if (request.getTime() != null) {
            int tolerance = request.getTimeToleranceMinutes() == null
                    ? riderDefaultToleranceMinutes
                    : request.getTimeToleranceMinutes();
            if (tolerance < 0 || tolerance > maximumRiderToleranceMinutes) {
                return false;
            }
            LocalDateTime preferred = LocalDateTime.of(requestedDate, request.getTime());
            riderStart = preferred.minusMinutes(tolerance);
            riderEnd = preferred.plusMinutes(tolerance);
        } else {
            LocalDateTime anchor = requestedDate.isAfter(today)
                    ? LocalDateTime.of(requestedDate, LocalDateTime.now(clock).toLocalTime())
                    : LocalDateTime.now(clock);
            riderStart = anchor.plusMinutes(minimumLeadTimeMinutes);
            riderEnd = riderStart.plusMinutes(defaultSearchWindowMinutes);
        }

        return !driverStart.isAfter(riderEnd) && !riderStart.isAfter(driverEnd);
    }
}
