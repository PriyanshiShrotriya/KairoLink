package com.KairoLink.repository;

import com.KairoLink.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByDriverIdOrderByDepartureTimeAsc(Long driverId);

    Optional<Ride> findByIdAndDriverId(Long id, Long driverId);
}
