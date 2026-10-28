package com.KairoLink.repository;

import com.KairoLink.entity.RoundTripRequest;
import com.KairoLink.entity.RoundTripRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoundTripRequestRepository extends JpaRepository<RoundTripRequest, Long> {

    @Query("""
        select request from RoundTripRequest request
        join fetch request.outboundBooking booking
        join fetch booking.ride ride
        join fetch ride.driver
        left join fetch request.confirmedReturnRide
        where booking.rider.id = :riderId
        order by request.createdAt desc
        """)
    List<RoundTripRequest> findByRiderIdOrderByCreatedAtDesc(@Param("riderId") Long riderId);

    @Query("""
        select request from RoundTripRequest request
        join fetch request.outboundBooking booking
        join fetch booking.rider
        join fetch booking.ride ride
        left join fetch request.confirmedReturnRide
        where ride.driver.id = :driverId
        order by request.createdAt desc
        """)
    List<RoundTripRequest> findByDriverIdOrderByCreatedAtDesc(@Param("driverId") Long driverId);

    boolean existsByOutboundBookingIdAndStatusIn(
            Long outboundBookingId, Collection<RoundTripRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request from RoundTripRequest request
        join fetch request.outboundBooking booking
        join fetch booking.ride ride
        join fetch ride.driver
        join fetch booking.rider
        where request.id = :id
        """)
    Optional<RoundTripRequest> findForUpdate(@Param("id") Long id);

    Optional<RoundTripRequest> findByIdAndOutboundBookingRiderId(Long id, Long riderId);

    @Query("""
        select request from RoundTripRequest request
        left join fetch request.confirmedReturnRide
        where request.outboundBooking.id = :bookingId
          and request.status = :status
        """)
    Optional<RoundTripRequest> findByOutboundBookingIdAndStatus(
            @Param("bookingId") Long bookingId,
            @Param("status") RoundTripRequestStatus status);
}
