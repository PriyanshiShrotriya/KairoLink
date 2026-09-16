package com.KairoLink;

import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.RideRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class RideFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/rides"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void riderCannotAccessRideManagement() throws Exception {
        mockMvc.perform(get("/rides").with(user("rider").roles("RIDER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void driverCanPublishAndViewOwnRide() throws Exception {
        User driver = saveUser("ride-driver@example.com", Role.DRIVER);
        String departure = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString();

        mockMvc.perform(post("/rides")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("source", "Campus")
                        .param("destination", "Office")
                        .param("departureTime", departure)
                        .param("seats", "3")
                        .param("price", "25.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rides?created"));

        Ride ride = rideRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals(driver.getId(), ride.getDriver().getId());
        assertEquals(RideStatus.ACTIVE, ride.getStatus());

        mockMvc.perform(get("/rides").with(user(driver.getEmail()).roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/my-rides"));
    }

    @Test
    @Transactional
    void invalidRideDataIsRejectedWithoutPersistence() throws Exception {
        User driver = saveUser("invalid-ride@example.com", Role.DRIVER);

        mockMvc.perform(post("/rides")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("source", "Campus")
                        .param("destination", "campus")
                        .param("departureTime", LocalDateTime.now().minusHours(1).toString())
                        .param("seats", "0")
                        .param("price", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/publish-ride"));

        assertEquals(0, rideRepository.count());
    }

    @Test
    @Transactional
    void anotherDriverCannotEditOrCancelRide() throws Exception {
        User owner = saveUser("ride-owner@example.com", Role.DRIVER);
        User other = saveUser("other-driver@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/rides/" + ride.getId())
                        .with(user(other.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("source", "New Source")
                        .param("destination", "New Destination")
                        .param("departureTime", LocalDateTime.now().plusDays(2).toString())
                        .param("seats", "4")
                        .param("price", "30.00"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/rides/" + ride.getId() + "/cancel")
                        .with(user(other.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void driverCanCancelEligibleRide() throws Exception {
        User owner = saveUser("cancel-ride@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/rides/" + ride.getId() + "/cancel")
                        .with(user(owner.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rides?cancelled"));

        assertEquals(RideStatus.CANCELLED,
                rideRepository.findById(ride.getId()).orElseThrow().getStatus());
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Ride User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private Ride saveRide(User driver, LocalDateTime departureTime) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource("Campus");
        ride.setDestination("Office");
        ride.setDepartureTime(departureTime);
        ride.setSeats(3);
        ride.setPrice(java.math.BigDecimal.ZERO);
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.saveAndFlush(ride);
    }
}
