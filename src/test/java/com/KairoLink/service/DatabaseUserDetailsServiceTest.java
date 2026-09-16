package com.KairoLink.service;

import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseUserDetailsServiceTest {

    private UserRepository userRepository;
    private DatabaseUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new DatabaseUserDetailsService(userRepository);
    }

    @Test
    void loadsUserByNormalizedEmail() {
        User user = user("person@example.com", Set.of(Role.RIDER), true);
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("  PERSON@EXAMPLE.COM ");

        assertEquals("person@example.com", userDetails.getUsername());
        assertEquals("password-hash", userDetails.getPassword());
        verify(userRepository).findByEmail("person@example.com");
    }

    @Test
    void throwsWhenEmailIsUnknown() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void mapsAllRolesToRoleAuthorities() {
        User user = user("admin@example.com",
                Set.of(Role.RIDER, Role.DRIVER, Role.ADMIN), true);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@example.com");

        assertEquals(Set.of("ROLE_RIDER", "ROLE_DRIVER", "ROLE_ADMIN"),
                userDetails.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void marksDisabledUserAsDisabled() {
        User user = user("disabled@example.com", Set.of(Role.RIDER), false);
        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled@example.com");

        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    private User user(String email, Set<Role> roles, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("password-hash");
        user.setRoles(roles);
        user.setEnabled(enabled);
        return user;
    }
}
