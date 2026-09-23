package com.KairoLink.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouteResponse(
        BigDecimal distanceMeters,
        BigDecimal durationSeconds,
        List<List<BigDecimal>> geometry) {
}
