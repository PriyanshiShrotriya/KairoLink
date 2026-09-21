package com.KairoLink.controller;

import com.KairoLink.dto.RideRequest;
import com.KairoLink.service.RideService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/rides/new")
    public String showPublishForm(Model model) {
        model.addAttribute("rideRequest", new RideRequest());
        return "driver/publish-ride";
    }

    @PostMapping("/rides")
    public String publish(
            Authentication authentication,
            @Valid @ModelAttribute("rideRequest") RideRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "driver/publish-ride";
        }
        rideService.publish(authentication.getName(), request);
        return "redirect:/rides?created";
    }

    @GetMapping("/rides")
    public String myRides(Authentication authentication, Model model) {
        model.addAttribute("rides", rideService.findMyRides(authentication.getName()));
        return "driver/my-rides";
    }

    @GetMapping("/rides/{id}/edit")
    public String showEditForm(
            Authentication authentication,
            @PathVariable Long id,
            Model model) {
        model.addAttribute("rideRequest", toRequest(rideService.getMyRide(authentication.getName(), id)));
        model.addAttribute("rideId", id);
        return "driver/edit-ride";
    }

    @PostMapping("/rides/{id}")
    public String update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @ModelAttribute("rideRequest") RideRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rideId", id);
            return "driver/edit-ride";
        }
        rideService.update(authentication.getName(), id, request);
        return "redirect:/rides?updated";
    }

    @PostMapping("/rides/{id}/cancel")
    public String cancel(Authentication authentication, @PathVariable Long id) {
        rideService.cancel(authentication.getName(), id);
        return "redirect:/rides?cancelled";
    }

    @PostMapping("/rides/{id}/start")
    public String start(Authentication authentication, @PathVariable Long id) {
        rideService.start(authentication.getName(), id);
        return "redirect:/rides?started";
    }

    @PostMapping("/rides/{id}/complete")
    public String complete(Authentication authentication, @PathVariable Long id) {
        rideService.complete(authentication.getName(), id);
        return "redirect:/rides?completed";
    }

    private RideRequest toRequest(com.KairoLink.entity.Ride ride) {
        RideRequest request = new RideRequest();
        request.setSource(ride.getSource());
        request.setDestination(ride.getDestination());
        request.setDepartureTime(ride.getDepartureTime());
        request.setSeats(ride.getSeats());
        request.setPrice(ride.getPrice());
        return request;
    }
}
