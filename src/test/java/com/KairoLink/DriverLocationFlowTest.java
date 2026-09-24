package com.KairoLink;

import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.repository.BookingRepository;
import com.KairoLink.repository.DriverLocationRepository;
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
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DriverLocationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private DriverLocationRepository locationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void ownerCanUpdateAndRetrieveOngoingRideLocation() throws Exception {
        User driver = saveUser("location-owner@example.com");
        Ride ride = saveRide(driver, RideStatus.ONGOING);

        mockMvc.perform(put("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"latitude":28.613900,"longitude":77.209000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139))
                .andExpect(jsonPath("$.longitude").value(77.209));

        assertEquals(1, locationRepository.count());

        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(driver.getEmail()).roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139))
                .andExpect(jsonPath("$.longitude").value(77.209));
    }

    @Test
    @Transactional
    void confirmedRiderCanRetrieveOngoingRideLocation() throws Exception {
        User driver = saveUser("location-confirmed-driver@example.com");
        User rider = saveRider("location-confirmed-rider@example.com");
        Ride ride = saveRide(driver, RideStatus.ONGOING);
        saveBooking(rider, ride, BookingStatus.CONFIRMED);
        saveLocation(ride);

        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139));
    }

    @Test
    @Transactional
    void nonConfirmedOrUnrelatedRiderCannotRetrieveLocation() throws Exception {
        User driver = saveUser("location-auth-driver@example.com");
        User rider = saveRider("location-pending-rider@example.com");
        User other = saveRider("location-unrelated-rider@example.com");
        Ride ride = saveRide(driver, RideStatus.ONGOING);
        saveBooking(rider, ride, BookingStatus.PENDING);
        saveLocation(ride);

        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(other.getEmail()).roles("RIDER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void confirmedRiderCannotRetrieveLocationForNonOngoingRide() throws Exception {
        User driver = saveUser("location-lifecycle-driver@example.com");
        User rider = saveRider("location-lifecycle-rider@example.com");
        Ride ride = saveRide(driver, RideStatus.ACTIVE);
        saveBooking(rider, ride, BookingStatus.CONFIRMED);
        saveLocation(ride);

        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void rejectedOrCancelledRiderCannotRetrieveLocation() throws Exception {
        User driver = saveUser("location-processed-driver@example.com");
        User rider = saveRider("location-processed-rider@example.com");
        Ride ride = saveRide(driver, RideStatus.ONGOING);
        Booking booking = saveBooking(rider, ride, BookingStatus.REJECTED);
        saveLocation(ride);

        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isNotFound());

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.saveAndFlush(booking);
        mockMvc.perform(get("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void nonOwnerCannotUpdateLocation() throws Exception {
        User owner = saveUser("location-owner-2@example.com");
        User other = saveUser("location-other@example.com");
        Ride ride = saveRide(owner, RideStatus.ONGOING);

        mockMvc.perform(put("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(other.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"latitude":28.613900,"longitude":77.209000}
                                """))
                .andExpect(status().isNotFound());
        assertEquals(0, locationRepository.count());
    }

    @Test
    @Transactional
    void locationCannotBeUpdatedForActiveRideOrInvalidCoordinates() throws Exception {
        User driver = saveUser("location-active@example.com");
        Ride ride = saveRide(driver, RideStatus.ACTIVE);

        mockMvc.perform(put("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"latitude":28.613900,"longitude":77.209000}
                                """))
                .andExpect(status().isNotFound());

        ride.setStatus(RideStatus.ONGOING);
        rideRepository.saveAndFlush(ride);
        mockMvc.perform(put("/api/rides/" + ride.getId() + "/driver-location")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"latitude":91,"longitude":181}
                                """))
                .andExpect(status().isBadRequest());
        assertEquals(0, locationRepository.count());
    }

    private User saveUser(String email) {
        return saveUser(email, Role.DRIVER);
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Location Driver");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private User saveRider(String email) {
        return saveUser(email, Role.RIDER);
    }

    private Booking saveBooking(User rider, Ride ride, BookingStatus status) {
        Booking booking = new Booking();
        booking.setRider(rider);
        booking.setRide(ride);
        booking.setStatus(status);
        booking.setSeatsRequested(1);
        booking.setTotalPrice(BigDecimal.ZERO);
        return bookingRepository.saveAndFlush(booking);
    }

    private void saveLocation(Ride ride) {
        com.KairoLink.entity.DriverLocation location = new com.KairoLink.entity.DriverLocation();
        location.setRide(ride);
        location.setLatitude(new BigDecimal("28.613900"));
        location.setLongitude(new BigDecimal("77.209000"));
        locationRepository.saveAndFlush(location);
    }

    private Ride saveRide(User driver, RideStatus status) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource("Campus");
        ride.setDestination("Office");
        ride.setDepartureTime(LocalDateTime.now().plusDays(1));
        ride.setSeats(3);
        ride.setPrice(BigDecimal.ZERO);
        ride.setStatus(status);
        return rideRepository.saveAndFlush(ride);
    }
}
