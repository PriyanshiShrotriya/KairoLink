package com.KairoLink;

import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.entity.Vehicle;
import com.KairoLink.repository.UserRepository;
import com.KairoLink.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/vehicle"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void riderAndAdminAreForbidden() throws Exception {
        mockMvc.perform(get("/vehicle").with(user("rider").roles("RIDER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/vehicle").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void driverCanViewEmptyVehiclePageAndAddVehicle() throws Exception {
        User driver = saveUser("vehicle-add@example.com", Role.DRIVER);

        mockMvc.perform(get("/vehicle").with(user(driver.getEmail()).roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/view"));

        mockMvc.perform(post("/vehicle")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", " Sedan ")
                        .param("numberPlate", " ABC 123 ")
                        .param("seats", "4")
                        .param("photoReference", " photo-ref "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicle?updated"));

        Vehicle vehicle = vehicleRepository.findByDriverId(driver.getId()).orElseThrow();
        assertEquals("Sedan", vehicle.getModel());
        assertEquals("ABC 123", vehicle.getNumberPlate());
        assertEquals(4, vehicle.getSeats());
        assertEquals("photo-ref", vehicle.getPhotoReference());
    }

    @Test
    @Transactional
    void driverCanEditExistingVehicle() throws Exception {
        User driver = saveUser("vehicle-edit@example.com", Role.DRIVER);
        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setModel("Old");
        vehicle.setNumberPlate("OLD");
        vehicle.setSeats(2);
        vehicleRepository.saveAndFlush(vehicle);

        mockMvc.perform(post("/vehicle")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", "Updated")
                        .param("numberPlate", "UPDATED")
                        .param("seats", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicle?updated"));

        assertEquals(1, vehicleRepository.findAll().stream()
                .filter(item -> driver.getId().equals(item.getDriver().getId()))
                .count());
        Vehicle updated = vehicleRepository.findByDriverId(driver.getId()).orElseThrow();
        assertEquals("Updated", updated.getModel());
        assertEquals(5, updated.getSeats());
    }

    @Test
    void postRequiresCsrf() throws Exception {
        mockMvc.perform(post("/vehicle")
                        .with(user("driver").roles("DRIVER"))
                        .param("model", "Sedan")
                        .param("numberPlate", "ABC")
                        .param("seats", "4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void invalidVehicleDataIsRejectedWithoutPersistence() throws Exception {
        User driver = saveUser("vehicle-invalid@example.com", Role.DRIVER);

        mockMvc.perform(post("/vehicle")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", " ")
                        .param("numberPlate", "")
                        .param("seats", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle/edit"));

        assertEquals(0, vehicleRepository.count());
    }

    @Test
    @Transactional
    void sameDriverKeepsOneVehicleAndCannotUseAnotherOwner() throws Exception {
        User driverOne = saveUser("vehicle-owner-one@example.com", Role.DRIVER);
        User driverTwo = saveUser("vehicle-owner-two@example.com", Role.DRIVER);

        mockMvc.perform(post("/vehicle")
                        .with(user(driverOne.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", "First")
                        .param("numberPlate", "ONE")
                        .param("seats", "4"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/vehicle")
                        .with(user(driverTwo.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", "Second")
                        .param("numberPlate", "TWO")
                        .param("seats", "5"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/vehicle")
                        .with(user(driverOne.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("model", "Updated First")
                        .param("numberPlate", "UPDATED")
                        .param("seats", "6")
                        .param("driverId", driverTwo.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertEquals(2, vehicleRepository.count());
        assertEquals("Updated First",
                vehicleRepository.findByDriverId(driverOne.getId()).orElseThrow().getModel());
        assertEquals("Second",
                vehicleRepository.findByDriverId(driverTwo.getId()).orElseThrow().getModel());
        assertNotNull(vehicleRepository.findByDriverId(driverOne.getId()).orElseThrow().getId());
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Vehicle User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }
}
