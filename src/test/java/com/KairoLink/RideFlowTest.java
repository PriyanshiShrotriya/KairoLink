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
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    void publishPageHidesCoordinateInputs() throws Exception {
        mockMvc.perform(get("/rides/new").with(user("publish-driver").roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"sourceLatitude\" type=\"hidden\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"destinationLongitude\" type=\"hidden\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "id=\"source\" type=\"text\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-use-current-location=\"true\"")));
    }

    @Test
    @Transactional
    void driverCanPublishRideWithCoordinates() throws Exception {
        User driver = saveUser("coordinate-driver@example.com", Role.DRIVER);
        String departure = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString();

        mockMvc.perform(post("/rides")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("source", "Campus")
                        .param("destination", "Office")
                        .param("sourceLatitude", "28.613900")
                        .param("sourceLongitude", "77.209000")
                        .param("destinationLatitude", "28.535500")
                        .param("destinationLongitude", "77.391000")
                        .param("departureTime", departure)
                        .param("seats", "3")
                        .param("price", "25.00"))
                .andExpect(status().is3xxRedirection());

        Ride ride = rideRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals(new BigDecimal("28.613900"), ride.getSourceLatitude());
        assertEquals(new BigDecimal("77.209000"), ride.getSourceLongitude());
        assertEquals(new BigDecimal("28.535500"), ride.getDestinationLatitude());
        assertEquals(new BigDecimal("77.391000"), ride.getDestinationLongitude());
    }

    @Test
    @Transactional
    void invalidCoordinateRangesAreRejectedWithoutPersistence() throws Exception {
        User driver = saveUser("invalid-coordinate-driver@example.com", Role.DRIVER);
        String departure = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString();

        mockMvc.perform(post("/rides")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .param("source", "Campus")
                        .param("destination", "Office")
                        .param("sourceLatitude", "91")
                        .param("destinationLongitude", "-181")
                        .param("departureTime", departure)
                        .param("seats", "3")
                        .param("price", "25.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/publish-ride"));

        assertEquals(0, rideRepository.count());
    }

    @Test
    @Transactional
    void editRidePageLoadsExistingCoordinatesIntoPicker() throws Exception {
        User driver = saveUser("edit-coordinate-driver@example.com", Role.DRIVER);
        Ride ride = saveRide(driver, LocalDateTime.now().plusDays(1));
        ride.setSourceLatitude(new BigDecimal("28.613900"));
        ride.setSourceLongitude(new BigDecimal("77.209000"));
        ride.setDestinationLatitude(new BigDecimal("28.535500"));
        ride.setDestinationLongitude(new BigDecimal("77.391000"));
        rideRepository.saveAndFlush(ride);

        mockMvc.perform(get("/rides/" + ride.getId() + "/edit")
                        .with(user(driver.getEmail()).roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(view().name("driver/edit-ride"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-source-latitude=\"28.613900\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-destination-longitude=\"77.391000\"")));
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

    @Test
    @Transactional
    void driverCanStartActiveRide() throws Exception {
        User owner = saveUser("start-ride@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/rides/" + ride.getId() + "/start")
                        .with(user(owner.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rides?started"));

        assertEquals(RideStatus.ONGOING,
                rideRepository.findById(ride.getId()).orElseThrow().getStatus());
    }

    @Test
    @Transactional
    void ongoingRidePageIncludesRouteCoordinatesForLiveMap() throws Exception {
        User owner = saveUser("ongoing-map@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));
        ride.setStatus(RideStatus.ONGOING);
        ride.setSourceLatitude(new BigDecimal("28.613900"));
        ride.setSourceLongitude(new BigDecimal("77.209000"));
        ride.setDestinationLatitude(new BigDecimal("28.535500"));
        ride.setDestinationLongitude(new BigDecimal("77.391000"));
        rideRepository.saveAndFlush(ride);

        mockMvc.perform(get("/rides").with(user(owner.getEmail()).roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-source-latitude=\"28.613900\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-destination-longitude=\"77.391000\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-driver-location-session")));
    }

    @Test
    @Transactional
    void driverCanCompleteOngoingRide() throws Exception {
        User owner = saveUser("complete-ride@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));
        ride.setStatus(RideStatus.ONGOING);
        rideRepository.saveAndFlush(ride);

        mockMvc.perform(post("/rides/" + ride.getId() + "/complete")
                        .with(user(owner.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rides?completed"));

        assertEquals(RideStatus.COMPLETED,
                rideRepository.findById(ride.getId()).orElseThrow().getStatus());
    }

    @Test
    @Transactional
    void anotherDriverCannotStartOrCompleteRide() throws Exception {
        User owner = saveUser("owner-driver@example.com", Role.DRIVER);
        User other = saveUser("intruder-driver@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/rides/" + ride.getId() + "/start")
                        .with(user(other.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        ride.setStatus(RideStatus.ONGOING);
        rideRepository.saveAndFlush(ride);

        mockMvc.perform(post("/rides/" + ride.getId() + "/complete")
                        .with(user(other.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void cannotStartNonActiveOrCompleteNonOngoingRide() throws Exception {
        User owner = saveUser("invalid-state@example.com", Role.DRIVER);
        Ride ride = saveRide(owner, LocalDateTime.now().plusDays(1));

        // Attempt to complete ACTIVE ride
        mockMvc.perform(post("/rides/" + ride.getId() + "/complete")
                        .with(user(owner.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        // Cancel ride, then attempt to start
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.saveAndFlush(ride);

        mockMvc.perform(post("/rides/" + ride.getId() + "/start")
                        .with(user(owner.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
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
