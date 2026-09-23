package com.KairoLink.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverLocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        Instant updatedAt) {
}
