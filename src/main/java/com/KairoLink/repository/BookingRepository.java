package com.KairoLink.repository;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findById(Long id);

        @Query("""
            select booking from Booking booking
            join fetch booking.ride ride
            join fetch ride.driver
            where booking.rider.id = :riderId
            order by booking.createdAt desc
            """)
        List<Booking> findByRiderIdOrderByCreatedAtDesc(@Param("riderId") Long riderId);

    List<Booking> findByRideId(Long rideId);

        @Query("""
            select booking from Booking booking
            join fetch booking.ride ride
            join fetch ride.driver
            join fetch booking.rider
            where ride.driver.id = :driverId
            order by booking.createdAt desc
            """)
        List<Booking> findByRideDriverIdOrderByCreatedAtDesc(@Param("driverId") Long driverId);

    Optional<Booking> findByIdAndRideDriverId(Long id, Long driverId);

    boolean existsByRiderIdAndRideIdAndStatusIn(
            Long riderId,
            Long rideId,
            Collection<BookingStatus> statuses);

    @Query("""
        select count(booking) from Booking booking
        where booking.createdAt >= :startOfWeek
        """)
    long countBookingsCreatedAfter(@Param("startOfWeek") Instant startOfWeek);

    @Query("""
        select booking from Booking booking
        join fetch booking.ride ride
        join fetch ride.driver
        join fetch booking.rider
        order by booking.createdAt desc
        """)
    List<Booking> findAllBookingsWithDetails();
}
