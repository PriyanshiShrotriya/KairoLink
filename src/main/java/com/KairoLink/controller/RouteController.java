package com.KairoLink.controller;

import com.KairoLink.dto.RouteResponse;
import com.KairoLink.exception.RouteCalculationException;
import com.KairoLink.service.OsrmRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final OsrmRouteService osrmRouteService;

    public RouteController(OsrmRouteService osrmRouteService) {
        this.osrmRouteService = osrmRouteService;
    }

    @GetMapping
    public ResponseEntity<?> calculate(
            @RequestParam BigDecimal sourceLatitude,
            @RequestParam BigDecimal sourceLongitude,
            @RequestParam BigDecimal destinationLatitude,
            @RequestParam BigDecimal destinationLongitude) {
        try {
            RouteResponse response = osrmRouteService.calculate(
                    sourceLatitude, sourceLongitude, destinationLatitude, destinationLongitude);
            return ResponseEntity.ok(response);
        } catch (RouteCalculationException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
