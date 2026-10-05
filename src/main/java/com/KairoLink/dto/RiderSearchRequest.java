package com.KairoLink.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class RiderSearchRequest {

    private String source;
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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime time;

    @Min(0)
    @Max(180)
    private Integer timeToleranceMinutes;

    @AssertTrue(message = "Time tolerance requires a preferred departure time")
    public boolean isTimeToleranceValid() {
        return timeToleranceMinutes == null || time != null;
    }

    @AssertTrue(message = "All pickup and destination coordinates must be provided together")
    public boolean isCoordinatePairComplete() {
        boolean anyCoordinate = sourceLatitude != null || sourceLongitude != null
                || destinationLatitude != null || destinationLongitude != null;
        boolean allCoordinates = sourceLatitude != null && sourceLongitude != null
                && destinationLatitude != null && destinationLongitude != null;
        return !anyCoordinate || allCoordinates;
    }
}
