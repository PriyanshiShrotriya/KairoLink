package com.KairoLink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "round_trip_requests")
@Getter
@Setter
@NoArgsConstructor
public class RoundTripRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbound_booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_round_trip_requests_outbound_booking"))
    private Booking outboundBooking;

    @Column(name = "requested_return_date", nullable = false)
    private LocalDate requestedReturnDate;

    @Column(name = "requested_return_time", nullable = false)
    private LocalTime requestedReturnTime;

    @Column(name = "return_pickup", nullable = false, length = 150)
    private String returnPickup;

    @Column(name = "return_destination", nullable = false, length = 150)
    private String returnDestination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoundTripRequestStatus status;

    @Column(name = "passenger_message", length = 1000)
    private String passengerMessage;

    @Column(name = "driver_response", length = 1000)
    private String driverResponse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_return_ride_id",
            foreignKey = @ForeignKey(name = "fk_round_trip_requests_return_ride"))
    private Ride confirmedReturnRide;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
