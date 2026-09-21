package com.KairoLink.service;

import com.KairoLink.entity.Rating;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.User;
import com.KairoLink.exception.DuplicateRatingException;
import com.KairoLink.exception.RideNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.RatingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    public RatingService(
            RatingRepository ratingRepository,
            RideRepository rideRepository,
            UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Rating createRating(Long rideId, Long reviewerId, Long reviewedUserId, int stars, String comment) {
        validateStars(stars);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride was not found"));
        validateRideCompleted(ride);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new UserNotFoundException("Reviewer was not found"));
        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new UserNotFoundException("Reviewed user was not found"));

        return saveRating(ride, reviewer, reviewedUser, stars, comment);
    }

    @Transactional
    public Rating createRating(String reviewerEmail, Long rideId, Long reviewedUserId, int stars, String comment) {
        validateStars(stars);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride was not found"));
        validateRideCompleted(ride);
        User reviewer = findUserByEmail(reviewerEmail);
        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new UserNotFoundException("Reviewed user was not found"));

        return saveRating(ride, reviewer, reviewedUser, stars, comment);
    }

    public double getAverageRating(Long userId) {
        validateUserExists(userId);
        List<Rating> ratings = ratingRepository.findByReviewedUserId(userId);
        return ratings.stream()
                .mapToInt(Rating::getStars)
                .average()
                .orElse(0.0);
    }

    public long getRatingCount(Long userId) {
        validateUserExists(userId);
        return ratingRepository.findByReviewedUserId(userId).size();
    }

    public List<Rating> getRatingsForUser(Long userId) {
        validateUserExists(userId);
        return ratingRepository.findByReviewedUserId(userId);
    }

    private Rating saveRating(Ride ride, User reviewer, User reviewedUser, int stars, String comment) {
        if (reviewer.getId().equals(reviewedUser.getId())) {
            throw new IllegalArgumentException("Users cannot rate themselves");
        }

        if (ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(
                ride.getId(), reviewer.getId(), reviewedUser.getId())) {
            throw new DuplicateRatingException("Rating has already been submitted for this ride and user");
        }

        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setReviewer(reviewer);
        rating.setReviewedUser(reviewedUser);
        rating.setStars(stars);
        rating.setComment(comment != null && !comment.trim().isEmpty() ? comment.trim() : null);

        return ratingRepository.save(rating);
    }

    private void validateStars(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }
    }

    private void validateRideCompleted(Ride ride) {
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalArgumentException("Ratings are only allowed after ride completion");
        }
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new UserNotFoundException("User was not found");
        }
    }

    private User findUserByEmail(String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UserNotFoundException("Reviewer was not found"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Reviewer was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
