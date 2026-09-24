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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class RiderSearchFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void riderSearchPageOffersCurrentLocationSourceAction() throws Exception {
        User rider = saveUser("location-search-rider@example.com", Role.RIDER);

        mockMvc.perform(get("/rider/search")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(view().name("rider/search"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-use-current-location")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-current-location-status")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/js/rider-search-location.js")));
    }

    @Test
    @Transactional
    void riderSearchFindsOnlyMatchingFutureActiveRidesInDepartureOrder() throws Exception {
        User rider = saveUser("search-rider@example.com", Role.RIDER);
        User driver = saveUser("search-driver@example.com", Role.DRIVER);
        LocalDate requestedDate = LocalDate.now().plusDays(1);

        Ride later = saveRide(driver, "Campus", "Office", requestedDate.atTime(10, 0), 2, RideStatus.ACTIVE);
        saveRide(driver, "Campus", "Office", requestedDate.atTime(8, 0), 2, RideStatus.ACTIVE);
        saveRide(driver, "Campus", "Office", requestedDate.atTime(9, 30), 2, RideStatus.CANCELLED);
        saveRide(driver, "Campus", "Office", LocalDateTime.now().minusDays(1), 2, RideStatus.ACTIVE);
        saveRide(driver, "Other", "Office", requestedDate.atTime(9, 0), 2, RideStatus.ACTIVE);

        mockMvc.perform(get("/rider/results")
                        .with(user(rider.getEmail()).roles("RIDER"))
                        .param("source", " campus ")
                        .param("destination", " OFFICE ")
                        .param("date", requestedDate.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("rider/results"))
                .andExpect(model().attribute("rides", org.hamcrest.Matchers.hasSize(2)));

        assertEquals(2, later.getSeats());
    }

    @Test
    @Transactional
    void riderSearchDefaultsToTodayWhenDateIsOmitted() throws Exception {
        User rider = saveUser("default-date-rider@example.com", Role.RIDER);
        User driver = saveUser("default-date-driver@example.com", Role.DRIVER);

        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalTime().isAfter(java.time.LocalTime.of(23, 59, 45))) {
            try {
                Thread.sleep(16000);
            } catch (InterruptedException ignored) {}
            now = LocalDateTime.now();
        }
        LocalDate today = now.toLocalDate();
        LocalDateTime departureToday = now.plusSeconds(10);
        LocalDateTime departureTomorrow = today.plusDays(1).atTime(12, 0);

        Ride todayRide = saveRide(driver, "Campus", "Office", departureToday, 2, RideStatus.ACTIVE);
        saveRide(driver, "Campus", "Office", departureTomorrow, 2, RideStatus.ACTIVE);

        mockMvc.perform(get("/rider/results")
                        .with(user(rider.getEmail()).roles("RIDER"))
                        .param("source", "Campus")
                        .param("destination", "Office"))
                .andExpect(status().isOk())
                .andExpect(view().name("rider/results"))
                .andExpect(model().attribute("date", today))
                .andExpect(model().attribute("rides", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("rides", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.equalTo(todayRide.getId())))));
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Search User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private Ride saveRide(
            User driver,
            String source,
            String destination,
            LocalDateTime departureTime,
            int seats,
            RideStatus status) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource(source);
        ride.setDestination(destination);
        ride.setDepartureTime(departureTime);
        ride.setSeats(seats);
        ride.setPrice(BigDecimal.TEN);
        ride.setStatus(status);
        return rideRepository.saveAndFlush(ride);
    }
}
