package com.KairoLink.controller;

import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.User;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.service.RatingService;
import com.KairoLink.service.RideService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequestMapping("/rides")
public class RatingController {

    private final RideService rideService;
    private final RatingService ratingService;
    private final UserRepository userRepository;

    public RatingController(RideService rideService, RatingService ratingService, UserRepository userRepository) {
        this.rideService = rideService;
        this.ratingService = ratingService;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/rate")
    public String showRatingForm(
            Authentication authentication,
            @PathVariable Long id,
            Model model) {
        Ride ride = rideService.getRide(id);

        if (ride.getStatus() != RideStatus.COMPLETED) {
            return "redirect:/rides?error=Only completed rides can be rated";
        }

        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        model.addAttribute("ride", ride);
        model.addAttribute("reviewedUsers",
                ratingService.getEligibleReviewedUsers(authentication.getName(), id));
        return "rating/form";
    }

    @PostMapping("/{id}/rate")
    public String submitRating(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam @Min(1) @Max(5) int stars,
            @RequestParam(required = false) String comment,
            @RequestParam Long reviewedUserId) {

        ratingService.createRating(
                authentication.getName(),
                id,
                reviewedUserId,
                stars,
                comment);

        return "redirect:/rides?rated";
    }
}
