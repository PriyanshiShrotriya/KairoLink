package com.KairoLink.service;

import com.KairoLink.dto.VehicleRequest;
import com.KairoLink.dto.VehicleView;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.entity.Vehicle;
import com.KairoLink.exception.VehicleNotFoundException;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleServiceTest {

    private UserRepository userRepository;
    private VehicleRepository vehicleRepository;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        vehicleService = new VehicleService(userRepository, vehicleRepository);
    }

    @Test
    void savesTrimmedFieldsForNormalizedDriverEmail() {
        User driver = driver("driver@example.com");
        VehicleRequest request = request(" Sedan ", " ABC 123 ", 4, " photo-ref ");
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId(driver.getId())).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleView vehicle = vehicleService.saveVehicle(" DRIVER@EXAMPLE.COM ", request);

        assertEquals("Sedan", vehicle.getModel());
        assertEquals("ABC 123", vehicle.getNumberPlate());
        assertEquals(4, vehicle.getSeats());
        assertEquals("photo-ref", vehicle.getPhotoReference());
        verify(vehicleRepository).findByDriverId(driver.getId());
    }

    @Test
    void updatesExistingVehicleInsteadOfCreatingSecondVehicle() {
        User driver = driver("driver@example.com");
        Vehicle existing = new Vehicle();
        existing.setDriver(driver);
        existing.setModel("Old");
        existing.setNumberPlate("OLD");
        existing.setSeats(2);
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriverId(driver.getId())).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleService.saveVehicle(driver.getEmail(), request("New", "NEW", 5, null));

        assertEquals("New", existing.getModel());
        assertEquals("NEW", existing.getNumberPlate());
        assertEquals(5, existing.getSeats());
        verify(vehicleRepository).save(existing);
    }

    @Test
    void rejectsNonDriverOwner() {
        User rider = new User();
        rider.setEmail("rider@example.com");
        rider.setRoles(Set.of(Role.RIDER));
        when(userRepository.findByEmail(rider.getEmail())).thenReturn(Optional.of(rider));

        assertThrows(VehicleNotFoundException.class,
                () -> vehicleService.getVehicle(rider.getEmail()));
    }

    private User driver(String email) {
        User driver = new User();
        driver.setId(1L);
        driver.setEmail(email);
        driver.setRoles(Set.of(Role.DRIVER));
        return driver;
    }

    private VehicleRequest request(String model, String numberPlate, int seats, String photoReference) {
        VehicleRequest request = new VehicleRequest();
        request.setModel(model);
        request.setNumberPlate(numberPlate);
        request.setSeats(seats);
        request.setPhotoReference(photoReference);
        return request;
    }
}
