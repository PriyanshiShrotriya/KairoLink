package com.KairoLink;

import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard/rider"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void riderCanAccessOnlyRiderDashboard() throws Exception {
        mockMvc.perform(get("/dashboard/rider").with(user("rider").roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/rider"));

        mockMvc.perform(get("/dashboard/driver").with(user("rider").roles("RIDER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard/admin").with(user("rider").roles("RIDER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void driverCanAccessOnlyDriverDashboard() throws Exception {
        mockMvc.perform(get("/dashboard/driver").with(user("driver").roles("DRIVER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/driver"));

        mockMvc.perform(get("/dashboard/rider").with(user("driver").roles("DRIVER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard/admin").with(user("driver").roles("DRIVER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void adminCanAccessOnlyAdminDashboard() throws Exception {
        mockMvc.perform(get("/dashboard/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/admin"));

        mockMvc.perform(get("/dashboard/rider").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard/driver").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @Transactional
    void successfulRiderAuthenticationReachesRiderDashboard() throws Exception {
        String email = "dashboard.rider@example.com";
        String password = "ValidPass1";
        saveUser(email, password, Role.RIDER);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        mockMvc.perform(get("/dashboard")
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/rider"));
    }

    private void saveUser(String email, String password, Role role) {
        User user = new User();
        user.setName("Dashboard User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        userRepository.saveAndFlush(user);
    }
}
