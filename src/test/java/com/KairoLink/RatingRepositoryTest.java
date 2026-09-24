package com.KairoLink;

import com.KairoLink.entity.Rating;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.RatingRepository;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RatingRepositoryTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void savesAndRetrievesRating() {
        User reviewer = saveUser("reviewer@example.com", Role.RIDER);
        User reviewedUser = saveUser("reviewed@example.com", Role.DRIVER);
        Ride ride = saveRide(reviewedUser);

        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setReviewer(reviewer);
        rating.setReviewedUser(reviewedUser);
        rating.setStars(5);
        rating.setComment("Great driver, on time!");

        Rating saved = ratingRepository.saveAndFlush(rating);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(5, saved.getStars());
        assertEquals("Great driver, on time!", saved.getComment());

        List<Rating> userRatings = ratingRepository.findByReviewedUserId(reviewedUser.getId());
        assertEquals(1, userRatings.size());
        assertEquals(saved.getId(), userRatings.get(0).getId());

        assertTrue(ratingRepository.existsByRideIdAndReviewerIdAndReviewedUserId(
                ride.getId(), reviewer.getId(), reviewedUser.getId()));
    }

    @Test
    @Transactional
    void preventsDuplicateRatingForSameRideReviewerAndReviewedUser() {
        User reviewer = saveUser("reviewer-dup@example.com", Role.RIDER);
        User reviewedUser = saveUser("reviewed-dup@example.com", Role.DRIVER);
        Ride ride = saveRide(reviewedUser);

        Rating rating1 = new Rating();
        rating1.setRide(ride);
        rating1.setReviewer(reviewer);
        rating1.setReviewedUser(reviewedUser);
        rating1.setStars(4);
        ratingRepository.saveAndFlush(rating1);

        Rating rating2 = new Rating();
        rating2.setRide(ride);
        rating2.setReviewer(reviewer);
        rating2.setReviewedUser(reviewedUser);
        rating2.setStars(5);

        assertThrows(DataIntegrityViolationException.class, () -> {
            ratingRepository.saveAndFlush(rating2);
        });
    }

    @Test
    @Transactional
    void rejectsStarsOutOfRange() {
        User reviewer = saveUser("reviewer-star@example.com", Role.RIDER);
        User reviewedUser = saveUser("reviewed-star@example.com", Role.DRIVER);
        Ride ride = saveRide(reviewedUser);

        Rating ratingLow = new Rating();
        ratingLow.setRide(ride);
        ratingLow.setReviewer(reviewer);
        ratingLow.setReviewedUser(reviewedUser);
        ratingLow.setStars(0);

        assertThrows(DataIntegrityViolationException.class, () -> {
            ratingRepository.saveAndFlush(ratingLow);
        });
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Rating Test User");
        user.setEmail(email);
        user.setPasswordHash("hashedpassword");
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private Ride saveRide(User driver) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource("Campus Gate");
        ride.setDestination("Metro Station");
        ride.setDepartureTime(LocalDateTime.now().plusHours(2));
        ride.setSeats(3);
        ride.setPrice(BigDecimal.valueOf(15.00));
        ride.setStatus(RideStatus.COMPLETED);
        return rideRepository.saveAndFlush(ride);
    }
}

