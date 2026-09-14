package com.KairoLink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RideSearchRequest {

    @NotBlank
    private String source;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDate date;
}
