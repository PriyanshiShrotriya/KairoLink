package com.KairoLink.controller;

import com.KairoLink.dto.GeocodingResponse;
import com.KairoLink.exception.GeocodingException;
import com.KairoLink.service.NominatimGeocodingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final NominatimGeocodingService geocodingService;

    public GeocodingController(NominatimGeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/reverse")
    public ResponseEntity<?> reverse(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude) {
        return execute(() -> geocodingService.reverse(latitude, longitude));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) {
        return execute(() -> geocodingService.search(query));
    }

    private ResponseEntity<?> execute(java.util.function.Supplier<GeocodingResponse> operation) {
        try {
            return ResponseEntity.ok(operation.get());
        } catch (GeocodingException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
