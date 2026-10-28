package com.KairoLink.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class RoundTripRequestForm {

    @NotBlank
    @Size(max = 150)
    private String returnPickup;

    @NotBlank
    @Size(max = 150)
    private String returnDestination;

    @NotNull
    @FutureOrPresent
    private LocalDate requestedReturnDate;

    @NotNull
    private LocalTime requestedReturnTime;

    @Size(max = 1000)
    private String passengerMessage;
}
