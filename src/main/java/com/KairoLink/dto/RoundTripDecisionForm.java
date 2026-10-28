package com.KairoLink.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class RoundTripDecisionForm {

    @NotNull
    @FutureOrPresent
    private LocalDate confirmedReturnDate;

    @NotNull
    private LocalTime confirmedReturnTime;

    public RoundTripDecisionForm(LocalDate date, LocalTime time) {
        confirmedReturnDate = date;
        confirmedReturnTime = time;
    }
}
