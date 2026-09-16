package com.KairoLink.service;

import com.KairoLink.dto.VehicleRequest;
import com.KairoLink.dto.VehicleView;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.entity.Vehicle;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.exception.VehicleNotFoundException;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class VehicleService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleService(UserRepository userRepository, VehicleRepository vehicleRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Optional<VehicleView> getVehicle(String email) {
        User driver = findDriver(email);
        return vehicleRepository.findByDriverId(driver.getId()).map(this::toView);
    }

    @Transactional
    public VehicleView saveVehicle(String email, VehicleRequest request) {
        User driver = findDriver(email);
        Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId())
                .orElseGet(Vehicle::new);

        vehicle.setDriver(driver);
        vehicle.setModel(request.getModel().trim());
        vehicle.setNumberPlate(request.getNumberPlate().trim());
        vehicle.setSeats(request.getSeats());
        vehicle.setPhotoReference(normalizeOptional(request.getPhotoReference()));

        return toView(vehicleRepository.save(vehicle));
    }

    public VehicleView getRequiredVehicle(String email) {
        return getVehicle(email)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle was not found"));
    }

    private User findDriver(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
        if (!user.getRoles().contains(Role.DRIVER)) {
            throw new VehicleNotFoundException("Driver vehicle was not found");
        }
        return user;
    }

    private VehicleView toView(Vehicle vehicle) {
        return new VehicleView(
                vehicle.getId(),
                vehicle.getModel(),
                vehicle.getNumberPlate(),
                vehicle.getSeats(),
                vehicle.getPhotoReference(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
