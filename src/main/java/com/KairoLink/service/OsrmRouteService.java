package com.KairoLink.service;

import com.KairoLink.dto.RouteResponse;
import com.KairoLink.exception.RouteCalculationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OsrmRouteService {

    private final RestClient restClient;

    public OsrmRouteService(
            RestClient.Builder restClientBuilder,
            @Value("${kairolink.osrm.base-url:https://router.project-osrm.org}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public RouteResponse calculate(BigDecimal sourceLatitude, BigDecimal sourceLongitude,
                                   BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        validateCoordinates(sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);
        String coordinates = sourceLongitude + "," + sourceLatitude + ";"
                + destinationLongitude + "," + destinationLatitude;
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/{coordinates}")
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .build(coordinates))
                    .retrieve()
                    .body(Map.class);
            return parseResponse(response);
        } catch (RouteCalculationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RouteCalculationException("Route service is unavailable", exception);
        }
    }

    private RouteResponse parseResponse(Map<?, ?> response) {
        Object routesValue = response == null ? null : response.get("routes");
        if (response == null || !"Ok".equals(response.get("code"))
                || !(routesValue instanceof List<?> routes) || routes.isEmpty()) {
            throw new RouteCalculationException("No route was found for these locations");
        }
        if (!(routes.get(0) instanceof Map<?, ?> route)
                || !(route.get("geometry") instanceof Map<?, ?> geometryNode)
                || !(geometryNode.get("coordinates") instanceof List<?> coordinates)
                || coordinates.isEmpty()) {
            throw new RouteCalculationException("Route geometry was not returned");
        }
        List<List<BigDecimal>> geometry = new ArrayList<>();
        for (Object pointValue : coordinates) {
            if (!(pointValue instanceof List<?> point) || point.size() < 2
                    || !(point.get(0) instanceof Number) || !(point.get(1) instanceof Number)) {
                throw new RouteCalculationException("Route geometry was invalid");
            }
            geometry.add(List.of(new BigDecimal(point.get(0).toString()),
                    new BigDecimal(point.get(1).toString())));
        }
        return new RouteResponse(
                decimalField(route, "distance"),
                decimalField(route, "duration"),
                geometry);
    }

    private BigDecimal decimalField(Map<?, ?> node, String field) {
        if (!(node.get(field) instanceof Number)) {
            throw new RouteCalculationException("Route response was incomplete");
        }
        return new BigDecimal(node.get(field).toString());
    }

    private void validateCoordinates(BigDecimal sourceLatitude, BigDecimal sourceLongitude,
                                     BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        if (sourceLatitude == null || sourceLongitude == null
                || destinationLatitude == null || destinationLongitude == null
                || !within(sourceLatitude, -90, 90) || !within(destinationLatitude, -90, 90)
                || !within(sourceLongitude, -180, 180) || !within(destinationLongitude, -180, 180)) {
            throw new RouteCalculationException("Route coordinates are invalid");
        }
    }

    private boolean within(BigDecimal value, int minimum, int maximum) {
        return value.compareTo(BigDecimal.valueOf(minimum)) >= 0
                && value.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }
}
