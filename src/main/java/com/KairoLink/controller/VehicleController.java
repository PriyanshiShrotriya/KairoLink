package com.KairoLink.controller;

import com.KairoLink.dto.VehicleRequest;
import com.KairoLink.dto.VehicleView;
import com.KairoLink.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/vehicle")
    public String viewVehicle(Authentication authentication, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicle(authentication.getName()).orElse(null));
        return "vehicle/view";
    }

    @GetMapping("/vehicle/edit")
    public String showEditForm(Authentication authentication, Model model) {
        VehicleRequest request = new VehicleRequest();
        vehicleService.getVehicle(authentication.getName()).ifPresent(vehicle -> copyToRequest(vehicle, request));
        model.addAttribute("vehicleRequest", request);
        return "vehicle/edit";
    }

    @PostMapping("/vehicle")
    public String saveVehicle(
            Authentication authentication,
            @Valid @ModelAttribute("vehicleRequest") VehicleRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "vehicle/edit";
        }

        vehicleService.saveVehicle(authentication.getName(), request);
        return "redirect:/vehicle?updated";
    }

    private void copyToRequest(VehicleView vehicle, VehicleRequest request) {
        request.setModel(vehicle.getModel());
        request.setNumberPlate(vehicle.getNumberPlate());
        request.setSeats(vehicle.getSeats());
        request.setPhotoReference(vehicle.getPhotoReference());
    }
}
