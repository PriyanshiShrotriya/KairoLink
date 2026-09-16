package com.KairoLink.dto;

import jakarta.validation.constraints.DecimalMin;
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
}
