package com.KairoLink.controller;

import com.KairoLink.dto.RideSearchRequest;
import com.KairoLink.service.RideSearchService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rider")
public class RiderController {

    private final RideSearchService rideSearchService;

    public RiderController(RideSearchService rideSearchService) {
        this.rideSearchService = rideSearchService;
    }

    @GetMapping("/search")
    public String searchForm(Model model) {
        if (!model.containsAttribute("searchRequest")) {
            model.addAttribute("searchRequest", new RideSearchRequest());
        }
        return "rider/search-ride";
    }

    @GetMapping("/search/results")
    public String search(
            @Valid @ModelAttribute("searchRequest") RideSearchRequest searchRequest,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "rider/search-ride";
        }
        model.addAttribute("rides", rideSearchService.search(searchRequest));
        return "rider/search-results";
    }

    @GetMapping("/rides/{rideId}")
    public String rideDetails(@PathVariable Long rideId, Model model) {
        model.addAttribute("ride", rideSearchService.findBookableRide(rideId));
        return "rider/ride-details";
    }
}
