package com.KairoLink.dto;

import com.KairoLink.entity.BookingStatus;

import java.time.Instant;

public record BookingView(
        Long id,
        Long rideId,
        BookingStatus status,
        Instant requestedAt,
        Instant updatedAt) {
}
