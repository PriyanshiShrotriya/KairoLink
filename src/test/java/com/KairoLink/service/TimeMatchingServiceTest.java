package com.KairoLink.service;

import com.KairoLink.dto.RiderSearchRequest;
import com.KairoLink.entity.Ride;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeMatchingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private final TimeMatchingService service = new TimeMatchingService(
            30, 30, 10, 30, 180, CLOCK);

    @Test
    void overlappingDriverAndRiderWindowsMatch() {
        assertTrue(matches(17, 0, TODAY, 17, 15, 10));
    }

    @Test
    void nonOverlappingWindowsAreRejected() {
        assertFalse(matches(17, 0, TODAY, 18, 0, 10));
    }

    @Test
    void riderToleranceExpandsTheWindow() {
        assertTrue(matches(17, 0, TODAY, 17, 45, 30));
        assertFalse(matches(17, 0, TODAY, 17, 45, 0));
    }

    @Test
    void defaultDriverFlexibilityIsApplied() {
        assertTrue(matches(17, 25, TODAY, 17, 55, 0));
    }

    @Test
    void missingPreferredTimeUsesLeadTimeAndSearchWindow() {
        RiderSearchRequest request = request(null);
        assertTrue(service.matches(ride(12, 15, TODAY), request, TODAY));
        assertFalse(service.matches(ride(13, 15, TODAY), request, TODAY));
    }

    @Test
    void expiredRideWindowIsRejected() {
        assertFalse(matches(11, 0, TODAY, null, null, null));
    }

    @Test
    void futureDateIsNotComparedWithTodaysCurrentTime() {
        LocalDate futureDate = TODAY.plusDays(1);
        assertTrue(matches(12, 15, futureDate, null, null, null));
    }

    @Test
    void midnightBoundaryUsesTheCorrectDate() {
        assertTrue(matches(0, 10, TODAY.plusDays(1), 0, 0, 30));
    }

    @Test
    void invalidToleranceIsRejected() {
        assertFalse(matches(17, 0, TODAY, 17, 0, 181));
        assertFalse(matches(17, 0, TODAY, 17, 0, -1));
    }

    private boolean matches(
            int driverHour,
            int driverMinute,
            LocalDate date,
            Integer riderHour,
            Integer riderMinute,
            Integer tolerance) {
        RiderSearchRequest request = request(
                riderHour == null ? null : riderHour + ":" + String.format("%02d", riderMinute));
        if (tolerance != null) {
            request.setTimeToleranceMinutes(tolerance);
        }
        return service.matches(ride(driverHour, driverMinute, date), request, date);
    }

    private RiderSearchRequest request(String time) {
        RiderSearchRequest request = new RiderSearchRequest();
        if (time != null) {
            request.setTime(java.time.LocalTime.parse(time, java.time.format.DateTimeFormatter.ofPattern("H:mm")));
        }
        return request;
    }

    private Ride ride(int hour, int minute, LocalDate date) {
        Ride ride = new Ride();
        ride.setDepartureTime(date.atTime(hour, minute));
        return ride;
    }
}
