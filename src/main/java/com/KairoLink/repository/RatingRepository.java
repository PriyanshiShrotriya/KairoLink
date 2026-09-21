package com.KairoLink.repository;

import com.KairoLink.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByReviewedUserId(Long reviewedUserId);

    List<Rating> findByRideId(Long rideId);

    Optional<Rating> findByRideIdAndReviewerIdAndReviewedUserId(
            Long rideId, Long reviewerId, Long reviewedUserId);

    boolean existsByRideIdAndReviewerIdAndReviewedUserId(
            Long rideId, Long reviewerId, Long reviewedUserId);
}

