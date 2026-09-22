package com.KairoLink.repository;

import com.KairoLink.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByDriverIdOrderByDepartureTimeAsc(Long driverId);

    Optional<Ride> findByIdAndDriverId(Long id, Long driverId);

        @Query("""
            select ride from Ride ride
          join fetch ride.driver
            where lower(trim(ride.source)) = lower(trim(:source))
              and lower(trim(ride.destination)) = lower(trim(:destination))
              and ride.departureTime >= :dayStart
              and ride.departureTime < :dayEnd
              and ride.departureTime > :now
              and ride.seats > 0
              and ride.status = com.KairoLink.entity.RideStatus.ACTIVE
            order by ride.departureTime asc
            """)
        List<Ride> searchAvailable(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd,
            @Param("now") LocalDateTime now);

            @Modifying(clearAutomatically = true, flushAutomatically = true)
            @Query("""
                update Ride ride
                set ride.seats = ride.seats - :requestedSeats
                where ride.id = :rideId
                  and ride.seats >= :requestedSeats
                  and ride.status = com.KairoLink.entity.RideStatus.ACTIVE
                """)
            int decrementSeatsIfAvailable(
                @Param("rideId") Long rideId,
                @Param("requestedSeats") int requestedSeats);
}
