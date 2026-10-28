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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.nio.file.Files;
import java.nio.file.Path;

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

        try {
            vehicleService.saveVehicle(authentication.getName(), request);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("vehiclePhoto", exception.getMessage());
            return "vehicle/edit";
        }
        return "redirect:/vehicle?updated";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handlePhotoTooLarge(
            Authentication authentication,
            Model model) {
        VehicleRequest request = new VehicleRequest();
        vehicleService.getVehicle(authentication.getName()).ifPresent(vehicle -> copyToRequest(vehicle, request));
        model.addAttribute("vehicleRequest", request);
        model.addAttribute("vehiclePhotoError", "Vehicle photos must be 5 MB or smaller.");
        return "vehicle/edit";
    }

    @GetMapping("/vehicle/photos/{filename:.+}")
    public ResponseEntity<Resource> vehiclePhoto(@PathVariable String filename) {
        Path path = vehicleService.resolvePhoto("/vehicle/photos/" + filename);
        if (path == null || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType = mediaType(filename);
        return ResponseEntity.ok().contentType(contentType).body(new FileSystemResource(path));
    }

    private MediaType mediaType(String filename) {
        String lower = filename.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    private void copyToRequest(VehicleView vehicle, VehicleRequest request) {
        request.setModel(vehicle.getModel());
        request.setNumberPlate(vehicle.getNumberPlate());
        request.setSeats(vehicle.getSeats());
        request.setPhotoReference(vehicle.getPhotoReference());
    }
}
