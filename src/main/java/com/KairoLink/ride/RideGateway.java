package com.KairoLink.ride;

import java.time.LocalDate;
import java.util.List;

/**
 * Integration boundary for the Phase 2 Ride implementation.
 *
 * No Ride entity, repository, status enum, or seat logic belongs in Phase 3.
 * The eventual adapter must make confirmSeat atomic and conditional on a
 * currently bookable ride with an available seat.
 */
public interface RideGateway {

    RideSummary findBookableRide(Long rideId);

    List<RideSummary> searchRides(String source, String destination, LocalDate date);

    boolean confirmSeat(Long rideId);
}
