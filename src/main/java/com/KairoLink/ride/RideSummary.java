package com.KairoLink.ride;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only data supplied by the Phase 2 Ride adapter. The adapter owns all
 * route, status, expiry, and seat rules.
 */
public record RideSummary(
        Long rideId,
        Long driverId,
        String driverName,
        String source,
        String destination,
        Instant departureTime,
        int totalSeats,
        int availableSeats,
        BigDecimal price,
        String status,
        String vehicleDescription) {
}
