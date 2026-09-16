package com.KairoLink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VehicleRequest {

    @NotBlank
    @Size(max = 100)
    private String model;

    @NotBlank
    @Size(max = 30)
    private String numberPlate;

    @NotNull
    @Positive
    private Integer seats;

    @Size(max = 255)
    private String photoReference;
}
