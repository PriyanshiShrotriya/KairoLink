package com.KairoLink.repository;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByRiderIdAndRideIdAndStatusIn(
            Long riderId, Long rideId, Collection<BookingStatus> statuses);

    List<Booking> findAllByRiderIdOrderByRequestedAtDesc(Long riderId);

    java.util.Optional<Booking> findByIdAndRiderId(Long id, Long riderId);
}
