package com.KairoLink.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class VehicleView {

    private final Long id;
    private final String model;
    private final String numberPlate;
    private final int seats;
    private final String photoReference;
    private final Instant createdAt;
    private final Instant updatedAt;

    public VehicleView(Long id, String model, String numberPlate, int seats,
                       String photoReference, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.model = model;
        this.numberPlate = numberPlate;
        this.seats = seats;
        this.photoReference = photoReference;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
