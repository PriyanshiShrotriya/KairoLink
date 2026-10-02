package com.KairoLink.service;

import com.KairoLink.dto.RouteResponse;
import com.KairoLink.entity.Ride;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RouteMatchingService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private final double proximityThresholdMeters;

    public RouteMatchingService(
            @Value("${kairolink.matching.route-proximity-meters:2000}") double proximityThresholdMeters) {
        if (proximityThresholdMeters < 0) {
            throw new IllegalArgumentException("Route proximity threshold cannot be negative");
        }
        this.proximityThresholdMeters = proximityThresholdMeters;
    }

    public boolean matches(
            Ride ride,
            RouteResponse route,
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude) {
        if (ride == null || route == null
                || pickupLatitude == null || pickupLongitude == null
                || destinationLatitude == null || destinationLongitude == null
                || route.geometry() == null || route.geometry().size() < 2) {
            return false;
        }

        Projection pickup = project(route.geometry(), pickupLatitude, pickupLongitude);
        Projection destination = project(route.geometry(), destinationLatitude, destinationLongitude);
        if (pickup == null || destination == null
                || pickup.outsideRoute() || destination.outsideRoute()
                || pickup.distanceMeters() > proximityThresholdMeters
                || destination.distanceMeters() > proximityThresholdMeters) {
            return false;
        }

        return pickup.positionMeters() < destination.positionMeters();
    }

    private Projection project(
            List<List<BigDecimal>> geometry,
            BigDecimal latitude,
            BigDecimal longitude) {
        double cumulativeMeters = 0;
        Projection nearest = null;

        for (int index = 0; index < geometry.size() - 1; index++) {
            Point start = point(geometry.get(index));
            Point end = point(geometry.get(index + 1));
            if (start == null || end == null) {
                return null;
            }

            double segmentLength = distanceMeters(start, end);
            double[] projection = projectOnSegment(start, end, latitude.doubleValue(), longitude.doubleValue());
            double distance = Math.hypot(projection[0], projection[1]);
            boolean outsideRoute = (index == 0 && projection[3] < 0)
                    || (index == geometry.size() - 2 && projection[3] > 1);
            if (nearest == null || distance < nearest.distanceMeters()) {
                nearest = new Projection(
                        distance,
                        cumulativeMeters + projection[2] * segmentLength,
                        outsideRoute);
            }
            cumulativeMeters += segmentLength;
        }
        return nearest;
    }

    private double[] projectOnSegment(
            Point start,
            Point end,
            double latitude,
            double longitude) {
        double referenceLatitude = Math.toRadians(latitude);
        double startX = metersLongitude(start.longitude() - longitude, referenceLatitude);
        double startY = metersLatitude(start.latitude() - latitude);
        double endX = metersLongitude(end.longitude() - longitude, referenceLatitude);
        double endY = metersLatitude(end.latitude() - latitude);
        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double lengthSquared = deltaX * deltaX + deltaY * deltaY;
        double rawT = lengthSquared == 0
                ? 0
                : -(startX * deltaX + startY * deltaY) / lengthSquared;
        double t = lengthSquared == 0
                ? 0
                : Math.clamp(rawT, 0, 1);
        double nearestX = startX + t * deltaX;
        double nearestY = startY + t * deltaY;
        return new double[]{nearestX, nearestY, t, rawT};
    }

    private double distanceMeters(Point first, Point second) {
        double referenceLatitude = Math.toRadians((first.latitude() + second.latitude()) / 2);
        double x = metersLongitude(second.longitude() - first.longitude(), referenceLatitude);
        double y = metersLatitude(second.latitude() - first.latitude());
        return Math.hypot(x, y);
    }

    private double metersLatitude(double latitudeDifference) {
        return Math.toRadians(latitudeDifference) * EARTH_RADIUS_METERS;
    }

    private double metersLongitude(double longitudeDifference, double referenceLatitude) {
        return Math.toRadians(longitudeDifference) * EARTH_RADIUS_METERS * Math.cos(referenceLatitude);
    }

    private Point point(List<BigDecimal> coordinate) {
        if (coordinate == null || coordinate.size() < 2
                || coordinate.get(0) == null || coordinate.get(1) == null) {
            return null;
        }
        return new Point(coordinate.get(1).doubleValue(), coordinate.get(0).doubleValue());
    }

    private record Point(double latitude, double longitude) {
    }

    private record Projection(double distanceMeters, double positionMeters, boolean outsideRoute) {
    }
}
