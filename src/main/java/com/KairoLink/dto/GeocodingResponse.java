package com.KairoLink.dto;

import java.math.BigDecimal;

public record GeocodingResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        String displayName) {
}
