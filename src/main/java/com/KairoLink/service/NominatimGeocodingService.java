package com.KairoLink.service;

import com.KairoLink.dto.GeocodingResponse;
import com.KairoLink.exception.GeocodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NominatimGeocodingService {

    private final RestClient restClient;

    public NominatimGeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${kairolink.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${kairolink.nominatim.user-agent:KairoLink/0.0.1}") String userAgent) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "en")
                .build();
    }

    public GeocodingResponse reverse(BigDecimal latitude, BigDecimal longitude) {
        validateCoordinates(latitude, longitude);
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .build())
                    .retrieve()
                    .body(Map.class);
            return parseResponse(response);
        } catch (GeocodingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeocodingException("Geocoding service is unavailable", exception);
        }
    }

    public GeocodingResponse search(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new GeocodingException("A location is required");
        }
        try {
            List<?> responses = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .queryParam("q", query.trim())
                            .build())
                    .retrieve()
                    .body(List.class);
            if (responses == null || responses.isEmpty()
                    || !(responses.get(0) instanceof Map<?, ?> response)) {
                throw new GeocodingException("No location was found");
            }
            return parseResponse(response);
        } catch (GeocodingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GeocodingException("Geocoding service is unavailable", exception);
        }
    }

    private GeocodingResponse parseResponse(Map<?, ?> response) {
        if (response == null || response.get("lat") == null || response.get("lon") == null) {
            throw new GeocodingException("No location was found");
        }
        try {
            BigDecimal latitude = new BigDecimal(response.get("lat").toString());
            BigDecimal longitude = new BigDecimal(response.get("lon").toString());
            return new GeocodingResponse(latitude, longitude, conciseName(response));
        } catch (NumberFormatException exception) {
            throw new GeocodingException("Geocoding response was invalid", exception);
        }
    }

    private String conciseName(Map<?, ?> response) {
        Object addressValue = response.get("address");
        if (addressValue instanceof Map<?, ?> address) {
            Set<String> parts = new LinkedHashSet<>();
            addPart(address, parts, "road");
            addPart(address, parts, "pedestrian");
            addPart(address, parts, "neighbourhood");
            addPart(address, parts, "suburb");
            addPart(address, parts, "city");
            addPart(address, parts, "town");
            addPart(address, parts, "village");
            addPart(address, parts, "state");
            addPart(address, parts, "postcode");
            String concise = String.join(", ", parts);
            if (!concise.isBlank()) {
                return limit(concise);
            }
        }
        Object displayName = response.get("display_name");
        if (displayName == null || displayName.toString().isBlank()) {
            throw new GeocodingException("Geocoding response was incomplete");
        }
        return limit(displayName.toString());
    }

    private void addPart(Map<?, ?> address, Set<String> parts, String key) {
        Object value = address.get(key);
        if (value != null && !value.toString().isBlank()) {
            parts.add(value.toString());
        }
    }

    private String limit(String value) {
        return value.length() <= 150 ? value : value.substring(0, 147).trim() + "...";
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new GeocodingException("Location coordinates are invalid");
        }
    }
}
