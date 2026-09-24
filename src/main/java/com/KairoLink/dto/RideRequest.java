package com.KairoLink.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RideRequest {

    @NotBlank
    @Size(max = 150)
    private String source;

    @NotBlank
    @Size(max = 150)
    private String destination;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal sourceLatitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal sourceLongitude;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal destinationLatitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal destinationLongitude;

    @NotNull
    @Future
    private LocalDateTime departureTime;

    @NotNull
    @Positive
    private Integer seats;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;

    @AssertTrue(message = "Source and destination must be different")
    public boolean isRouteDistinct() {
        if (source == null || destination == null) {
            return true;
        }
        return !source.trim().equalsIgnoreCase(destination.trim());
    }

    @AssertTrue(message = "All route coordinates must be provided together")
    public boolean isCoordinatePairComplete() {
        boolean anyCoordinate = sourceLatitude != null || sourceLongitude != null
                || destinationLatitude != null || destinationLongitude != null;
        boolean allCoordinates = sourceLatitude != null && sourceLongitude != null
                && destinationLatitude != null && destinationLongitude != null;
        return !anyCoordinate || allCoordinates;
    }
}
