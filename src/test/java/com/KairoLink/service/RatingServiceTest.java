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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RatingServiceTest {

    private RatingRepository ratingRepository;
    private RideRepository rideRepository;
    private UserRepository userRepository;
    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(RatingRepository.class);
        rideRepository = mock(RideRepository.class);
        userRepository = mock(UserRepository.class);
        ratingService = new RatingService(ratingRepository, rideRepository, userRepository);
    }

    @Test
    void createsRatingSuccessfullyByUserId() {
        Ride ride = ride(10L);
        User reviewer = user(1L, "rider@example.com");
        User reviewedUser = user(2L, "driver@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewedUser));
        when(ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(10L, 1L, 2L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Rating rating = ratingService.createRating(10L, 1L, 2L, 5, "Smooth and friendly drive!");

        assertNotNull(rating);
        assertEquals(ride, rating.getRide());
        assertEquals(reviewer, rating.getReviewer());
        assertEquals(reviewedUser, rating.getReviewedUser());
        assertEquals(5, rating.getStars());
        assertEquals("Smooth and friendly drive!", rating.getComment());
        verify(ratingRepository).save(any(Rating.class));
    }

    @Test
    void acceptsRatingForCompletedRide() {
        Ride ride = ride(10L);
        ride.setStatus(RideStatus.COMPLETED);
        User reviewer = user(1L, "rider@example.com");
        User reviewedUser = user(2L, "driver@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewedUser));
        when(ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(10L, 1L, 2L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Rating rating = ratingService.createRating(10L, 1L, 2L, 5, "Completed ride");

        assertEquals(RideStatus.COMPLETED, rating.getRide().getStatus());
        verify(ratingRepository).save(any(Rating.class));
    }

    @ParameterizedTest
    @MethodSource("nonCompletedRideStatuses")
    void rejectsRatingForNonCompletedRide(RideStatus status) {
        Ride ride = ride(10L);
        ride.setStatus(status);
        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));

        assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 5, "Not completed"));
    }

    private static Stream<Arguments> nonCompletedRideStatuses() {
        return Stream.of(
                Arguments.of(RideStatus.CREATED),
                Arguments.of(RideStatus.ACTIVE),
                Arguments.of(RideStatus.ONGOING),
                Arguments.of(RideStatus.CANCELLED));
    }

    @Test
    void createsRatingSuccessfullyByReviewerEmail() {
        Ride ride = ride(10L);
        User reviewer = user(1L, "rider@example.com");
        User reviewedUser = user(2L, "driver@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findByEmail("rider@example.com")).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewedUser));
        when(ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(10L, 1L, 2L)).thenReturn(false);
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Rating rating = ratingService.createRating(" RIDER@EXAMPLE.COM ", 10L, 2L, 4, " Good ride ");

        assertEquals(4, rating.getStars());
        assertEquals("Good ride", rating.getComment());
        assertEquals(reviewer, rating.getReviewer());
        verify(ratingRepository).save(any(Rating.class));
    }

    @Test
    void rejectsStarsBelowOne() {
        assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 0, "Too low"));
    }

    @Test
    void rejectsStarsAboveFive() {
        assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 6, "Too high"));
    }

    @Test
    void rejectsDuplicateRating() {
        Ride ride = ride(10L);
        User reviewer = user(1L, "rider@example.com");
        User reviewedUser = user(2L, "driver@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewedUser));
        when(ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(10L, 1L, 2L)).thenReturn(true);

        assertThrows(DuplicateRatingException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 5, "Repeat"));
    }

    @Test
    void rejectsSelfRating() {
        Ride ride = ride(10L);
        User sameUser = user(1L, "same@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sameUser));

        assertThrows(IllegalArgumentException.class, () ->
                ratingService.createRating(10L, 1L, 1L, 5, "Rating myself"));
    }

    @Test
    void rejectsNonExistentRide() {
        when(rideRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () ->
                ratingService.createRating(99L, 1L, 2L, 5, "Ride missing"));
    }

    @Test
    void rejectsNonExistentReviewer() {
        Ride ride = ride(10L);
        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 5, "Reviewer missing"));
    }

    @Test
    void rejectsNonExistentReviewedUser() {
        Ride ride = ride(10L);
        User reviewer = user(1L, "reviewer@example.com");

        when(rideRepository.findById(10L)).thenReturn(Optional.of(ride));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                ratingService.createRating(10L, 1L, 2L, 5, "Reviewed user missing"));
    }

    @Test
    void computesAverageRatingAndCount() {
        when(userRepository.existsById(2L)).thenReturn(true);

        Rating r1 = new Rating();
        r1.setStars(4);
        Rating r2 = new Rating();
        r2.setStars(5);

        when(ratingRepository.findByReviewedUserId(2L)).thenReturn(List.of(r1, r2));

        assertEquals(4.5, ratingService.getAverageRating(2L));
        assertEquals(2, ratingService.getRatingCount(2L));
    }

    @Test
    void computesZeroAverageWhenNoRatings() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(ratingRepository.findByReviewedUserId(2L)).thenReturn(Collections.emptyList());

        assertEquals(0.0, ratingService.getAverageRating(2L));
        assertEquals(0, ratingService.getRatingCount(2L));
    }

    @Test
    void rejectsAverageRatingForNonExistentUser() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () ->
                ratingService.getAverageRating(99L));
        assertThrows(UserNotFoundException.class, () ->
                ratingService.getRatingCount(99L));
    }

    private Ride ride(Long id) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setStatus(RideStatus.COMPLETED);
        return ride;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
