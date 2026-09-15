package com.KairoLink;

import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    private static final String EMAIL = "login@example.com";
    private static final String PASSWORD = "ValidPass1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @Transactional
    void successfulAuthenticationRedirectsToHomeAndCreatesSession() throws Exception {
        saveUser(true);

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "  LOGIN@EXAMPLE.COM ")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        SecurityContext securityContext = (SecurityContext) result.getRequest()
                .getSession()
                .getAttribute("SPRING_SECURITY_CONTEXT");
        assertNotNull(securityContext);
        assertEquals(EMAIL, securityContext.getAuthentication().getName());

        mockMvc.perform(get("/future-authenticated-route")
                        .session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void failedAuthenticationRedirectsWithoutExposingPassword() throws Exception {
        saveUser(true);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", "WrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @Transactional
    void logoutInvalidatesAuthenticatedSessionAndRedirectsHome() throws Exception {
        saveUser(true);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(post("/logout")
                        .with(csrf())
                        .session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    private void saveUser(boolean enabled) {
        User user = new User();
        user.setName("Login User");
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRoles(Set.of(Role.RIDER));
        user.setEnabled(enabled);
        userRepository.saveAndFlush(user);
    }
}
