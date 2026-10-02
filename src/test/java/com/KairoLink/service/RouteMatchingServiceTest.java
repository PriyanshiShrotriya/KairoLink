package com.KairoLink.service;

import com.KairoLink.dto.RouteResponse;
import com.KairoLink.entity.Ride;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteMatchingServiceTest {

    private final RouteMatchingService matchingService = new RouteMatchingService(2_000);

    @Test
    void acceptsPickupAndDropOnRouteInOrder() {
        assertTrue(matches(route(), 0.25, 0, 0.75, 0));
    }

    @Test
    void rejectsPickupOutsideThreshold() {
        assertFalse(matches(route(), 0.25, 0.03, 0.75, 0));
    }

    @Test
    void rejectsDestinationOutsideThreshold() {
        assertFalse(matches(route(), 0.25, 0, 0.75, 0.03));
    }

    @Test
    void rejectsDropBeforePickup() {
        assertFalse(matches(route(), 0.75, 0, 0.25, 0));
    }

    @Test
    void rejectsLocationsNearButNotOnTheActualRoute() {
        RouteResponse bentRoute = new RouteResponse(
                new BigDecimal("30000"),
                BigDecimal.ONE,
                List.of(
                        point(0, 0),
                        point(1, 0),
                        point(1, 1)));

        assertFalse(matchingService.matches(
                new Ride(), bentRoute,
                decimal(0.5), decimal(0.01),
                decimal(0.5), decimal(0.99)));
    }

    @Test
    void rejectsPickupAfterDriverDestination() {
        assertFalse(matches(route(), 1.01, 0, 1.02, 0));
    }

    @Test
    void rejectsDropAfterDriverDestination() {
        assertFalse(matches(route(), 0.5, 0, 1.01, 0));
    }

    private boolean matches(
            RouteResponse route,
            double pickupLatitude,
            double pickupLongitude,
            double destinationLatitude,
            double destinationLongitude) {
        return matchingService.matches(
                new Ride(),
                route,
                decimal(pickupLatitude),
                decimal(pickupLongitude),
                decimal(destinationLatitude),
                decimal(destinationLongitude));
    }

    private RouteResponse route() {
        return new RouteResponse(
                decimal(111_194.9),
                BigDecimal.ONE,
                List.of(point(0, 0), point(1, 0)));
    }

    private List<BigDecimal> point(double latitude, double longitude) {
        return List.of(decimal(longitude), decimal(latitude));
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
