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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @Transactional
    void authenticatedUserCanViewProfileAndEditForm() throws Exception {
        User user = saveUser();

        mockMvc.perform(get("/profile").with(user(user.getEmail()).roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"));

        mockMvc.perform(get("/profile/edit").with(user(user.getEmail()).roles("RIDER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    @Test
    @Transactional
    void validUpdatePersistsEditableFieldsAndRedirects() throws Exception {
        User user = saveUser();
        String originalHash = user.getPasswordHash();

        mockMvc.perform(post("/profile")
                        .with(user(user.getEmail()).roles("RIDER"))
                        .with(csrf())
                        .param("name", "Updated Person")
                        .param("phone", "9876543210")
                        .param("profilePhotoReference", "photo-ref"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        User updated = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertEquals("Updated Person", updated.getName());
        assertEquals("9876543210", updated.getPhone());
        assertEquals("photo-ref", updated.getProfilePhotoReference());
        assertEquals(user.getEmail(), updated.getEmail());
        assertEquals(originalHash, updated.getPasswordHash());
        assertEquals(Set.of(Role.RIDER), updated.getRoles());
        assertTrue(updated.isEnabled());
    }

    @Test
    void rejectsUpdateWithoutCsrf() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(user("person@example.com").roles("RIDER"))
                        .param("name", "Updated Person"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void invalidUpdateReturnsEditView() throws Exception {
        User user = saveUser();

        mockMvc.perform(post("/profile")
                        .with(user(user.getEmail()).roles("RIDER"))
                        .with(csrf())
                        .param("name", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    private User saveUser() {
        User user = new User();
        user.setName("Person");
        user.setEmail("profile@example.com");
        user.setPasswordHash(passwordEncoder.encode("ValidPass1"));
        user.setRoles(Set.of(Role.RIDER));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }
}
