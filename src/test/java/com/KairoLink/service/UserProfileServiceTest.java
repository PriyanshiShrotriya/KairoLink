package com.KairoLink.service;

import com.KairoLink.dto.ProfileUpdateRequest;
import com.KairoLink.dto.ProfileView;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {

    private UserRepository userRepository;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userProfileService = new UserProfileService(userRepository);
    }

    @Test
    void loadsProfileUsingNormalizedEmail() {
        User user = user("person@example.com");
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        ProfileView profile = userProfileService.getProfile(" PERSON@EXAMPLE.COM ");

        assertEquals("person@example.com", profile.getEmail());
        assertEquals("Person", profile.getName());
        verify(userRepository).findByEmail("person@example.com");
    }

    @Test
    void updatesEditableFieldsAndPreservesSecurityFields() {
        User user = user("person@example.com");
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(" Updated Person ");
        request.setPhone(" 9876543210 ");
        request.setProfilePhotoReference(" photo-ref ");
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileView profile = userProfileService.updateProfile("person@example.com", request);

        assertEquals("Updated Person", profile.getName());
        assertEquals("9876543210", profile.getPhone());
        assertEquals("photo-ref", profile.getProfilePhotoReference());
        assertEquals("person@example.com", profile.getEmail());
        assertEquals("password-hash", user.getPasswordHash());
        assertEquals(Set.of(Role.RIDER), user.getRoles());
        verify(userRepository).save(user);
    }

    @Test
    void normalizesBlankOptionalFieldsToNull() {
        User user = user("person@example.com");
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("Updated Person");
        request.setPhone(" ");
        request.setProfilePhotoReference(" ");
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userProfileService.updateProfile("person@example.com", request);

        assertEquals(null, user.getPhone());
        assertEquals(null, user.getProfilePhotoReference());
    }

    @Test
    void throwsWhenAuthenticatedUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userProfileService.getProfile("missing@example.com"));
    }

    private User user(String email) {
        User user = new User();
        user.setName("Person");
        user.setEmail(email);
        user.setPasswordHash("password-hash");
        user.setRoles(Set.of(Role.RIDER));
        user.setEnabled(true);
        return user;
    }
}
