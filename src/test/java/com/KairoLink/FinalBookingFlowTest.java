package com.KairoLink;

import com.KairoLink.entity.Booking;
import com.KairoLink.entity.BookingStatus;
import com.KairoLink.entity.Ride;
import com.KairoLink.entity.RideStatus;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.BookingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class FinalBookingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void driverCanAcceptOwnRideBookingAndSeatsDecreaseOnce() throws Exception {
        User driver = saveUser("final-driver@example.com", Role.DRIVER);
        User rider = saveUser("final-rider@example.com", Role.RIDER);
        Ride ride = saveRide(driver, 3);
        Booking booking = saveBooking(rider, ride, BookingStatus.PENDING, 2);

        mockMvc.perform(post("/driver/bookings/" + booking.getId() + "/accept")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/driver/bookings?accepted"));

        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(booking.getId()).orElseThrow().getStatus());
        assertEquals(1, rideRepository.findById(ride.getId()).orElseThrow().getSeats());

        mockMvc.perform(post("/driver/bookings/" + booking.getId() + "/accept")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertEquals(1, rideRepository.findById(ride.getId()).orElseThrow().getSeats());
    }

    @Test
    @Transactional
    void acceptingAllRemainingSeatsReturnsConflictWithoutDatabaseError() throws Exception {
        User driver = saveUser("boundary-driver@example.com", Role.DRIVER);
        User rider = saveUser("boundary-rider@example.com", Role.RIDER);
        Ride ride = saveRide(driver, 2);
        Booking booking = saveBooking(rider, ride, BookingStatus.PENDING, 2);

        mockMvc.perform(post("/driver/bookings/" + booking.getId() + "/accept")
                        .with(user(driver.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertEquals(BookingStatus.PENDING,
                bookingRepository.findById(booking.getId()).orElseThrow().getStatus());
        assertEquals(2, rideRepository.findById(ride.getId()).orElseThrow().getSeats());
    }

    @Test
    @Transactional
    void riderSeesOnlyOwnBookingsAndDriverManagementIsRoleProtected() throws Exception {
        User driver = saveUser("visibility-driver@example.com", Role.DRIVER);
        User rider = saveUser("visibility-rider@example.com", Role.RIDER);
        User otherRider = saveUser("visibility-other@example.com", Role.RIDER);
        Ride ride = saveRide(driver, 3);
        saveBooking(rider, ride, BookingStatus.PENDING, 1);
        saveBooking(otherRider, ride, BookingStatus.REJECTED, 1);

        mockMvc.perform(get("/rider/bookings").with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(view().name("rider/my-bookings"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attribute("bookings", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(get("/driver/bookings").with(user(rider.getEmail()).roles("RIDER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/rider/bookings").with(user(driver.getEmail()).roles("DRIVER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @Transactional
    void driverCannotManageAnotherDriversBooking() throws Exception {
        User owner = saveUser("owner-driver@example.com", Role.DRIVER);
        User otherDriver = saveUser("other-driver@example.com", Role.DRIVER);
        User rider = saveUser("owner-rider@example.com", Role.RIDER);
        Ride ride = saveRide(owner, 2);
        Booking booking = saveBooking(rider, ride, BookingStatus.PENDING, 1);

        mockMvc.perform(post("/driver/bookings/" + booking.getId() + "/reject")
                        .with(user(otherDriver.getEmail()).roles("DRIVER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setName("Final Flow User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private Ride saveRide(User driver, int seats) {
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setSource("Campus");
        ride.setDestination("Office");
        ride.setDepartureTime(LocalDateTime.now().plusDays(1));
        ride.setSeats(seats);
        ride.setPrice(new BigDecimal("25.00"));
        ride.setStatus(RideStatus.ACTIVE);
        return rideRepository.saveAndFlush(ride);
    }

    private Booking saveBooking(User rider, Ride ride, BookingStatus status, int seats) {
        Booking booking = new Booking();
        booking.setRider(rider);
        booking.setRide(ride);
        booking.setStatus(status);
        booking.setSeatsRequested(seats);
        booking.setTotalPrice(ride.getPrice().multiply(BigDecimal.valueOf(seats)));
        return bookingRepository.saveAndFlush(booking);
    }
}
