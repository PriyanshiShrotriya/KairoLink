package com.KairoLink;

import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void registersRiderThroughHttpAndPersistsExpectedData() throws Exception {
        String rawPassword = "ValidPass1";

        mockMvc.perform(post("/register")
                        .param("name", "Priya Shrotriya")
                .param("email", "Rider.Integration@example.com")
                        .param("phone", "  9876543210  ")
                        .param("password", rawPassword)
                        .param("confirmPassword", rawPassword)
                        .param("role", "RIDER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?success"));

        assertEquals(1, userRepository.count());
        User user = userRepository.findByEmail("rider.integration@example.com").orElseThrow();
        assertEquals("rider.integration@example.com", user.getEmail());
        assertEquals("9876543210", user.getPhone());
        assertFalse(user.getPasswordHash().equals(rawPassword));
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()));
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(Role.RIDER));
        assertFalse(user.getRoles().contains(Role.ADMIN));
    }
}
