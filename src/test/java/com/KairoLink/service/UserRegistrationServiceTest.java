package com.KairoLink.service;

import com.KairoLink.dto.RegistrationRequest;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.DuplicateEmailException;
import com.KairoLink.exception.InvalidRegistrationException;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new UserRegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void registersRiderWithNormalizedValuesEncodedPasswordAndOneRole() {
        RegistrationRequest request = validRequest(Role.RIDER);
        request.setName("  Priya Shrotriya  ");
        request.setEmail("  PERSON@Example.COM  ");
        request.setPhone("  9876543210  ");

        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Priya Shrotriya", savedUser.getName());
        assertEquals("person@example.com", savedUser.getEmail());
        assertEquals("9876543210", savedUser.getPhone());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertNotEquals("ValidPass1", savedUser.getPasswordHash());
        assertEquals(Set.of(Role.RIDER), savedUser.getRoles());
        verify(userRepository).existsByEmail("person@example.com");
    }

    @Test
    void registersDriverWithExactlyOneDriverRole() {
        RegistrationRequest request = validRequest(Role.DRIVER);

        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        assertEquals(Set.of(Role.DRIVER), userCaptor.getValue().getRoles());
    }

    @ParameterizedTest
    @MethodSource("phoneValues")
    void normalizesPhoneValues(String submittedPhone, String expectedPhone) {
        RegistrationRequest request = validRequest(Role.RIDER);
        request.setPhone(submittedPhone);

        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(expectedPhone, userCaptor.getValue().getPhone());
    }

    private static Stream<Arguments> phoneValues() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("   ", null),
                Arguments.of("  9876543210  ", "9876543210")
        );
    }

    @Test
    void rejectsDuplicateEmailBeforeEncodingAndPersistence() {
        RegistrationRequest request = validRequest(Role.RIDER);
        when(userRepository.existsByEmail("person@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.register(request));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsPasswordMismatchBeforeRepositoryLookup() {
        RegistrationRequest request = validRequest(Role.RIDER);
        request.setConfirmPassword("Different1");

        assertThrows(InvalidRegistrationException.class, () -> service.register(request));

        verifyNoRegistrationInteractions();
    }

    @Test
    void rejectsPasswordShorterThanEightCharactersBeforeRepositoryLookup() {
        RegistrationRequest request = validRequest(Role.RIDER);
        request.setPassword("short7!");
        request.setConfirmPassword("short7!");

        assertThrows(InvalidRegistrationException.class, () -> service.register(request));

        verifyNoRegistrationInteractions();
    }

    @Test
    void rejectsPasswordExceedingBcryptUtf8ByteLimitBeforeEncoding() {
        String longUtf8Password = "密码".repeat(25);
        RegistrationRequest request = validRequest(Role.RIDER);
        request.setPassword(longUtf8Password);
        request.setConfirmPassword(longUtf8Password);

        assertThrows(InvalidRegistrationException.class, () -> service.register(request));

        verifyNoRegistrationInteractions();
    }

    @Test
    void rejectsAdminRegistrationBeforeRepositoryLookup() {
        RegistrationRequest request = validRequest(Role.ADMIN);

        assertThrows(InvalidRegistrationException.class, () -> service.register(request));

        verifyNoRegistrationInteractions();
    }

    @Test
    void encodesPasswordBeforePersistingUser() {
        RegistrationRequest request = validRequest(Role.RIDER);
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");

        service.register(request);

        InOrder inOrder = inOrder(passwordEncoder, userRepository);
        inOrder.verify(passwordEncoder).encode("ValidPass1");
        inOrder.verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void translatesKnownEmailConstraintViolationAndPreservesCause() {
        RegistrationRequest request = validRequest(Role.RIDER);
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");
        SQLException cause = new SQLException(
                "duplicate key value violates unique constraint \"uk_users_email\"",
                "23505"
        );
        DataIntegrityViolationException integrityViolation =
                new DataIntegrityViolationException("duplicate email", cause);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(integrityViolation);

        DuplicateEmailException exception =
                assertThrows(DuplicateEmailException.class, () -> service.register(request));

        assertEquals(integrityViolation, exception.getCause());
    }

    @Test
    void rethrowsUnrelatedIntegrityViolationUnchanged() {
        RegistrationRequest request = validRequest(Role.RIDER);
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded-password");
        DataIntegrityViolationException integrityViolation =
                new DataIntegrityViolationException("unrelated constraint");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(integrityViolation);

        DataIntegrityViolationException exception =
                assertThrows(DataIntegrityViolationException.class, () -> service.register(request));

        assertEquals(integrityViolation, exception);
    }

    private RegistrationRequest validRequest(Role role) {
        RegistrationRequest request = new RegistrationRequest();
        request.setName("Priya Shrotriya");
        request.setEmail("person@example.com");
        request.setPhone(null);
        request.setPassword("ValidPass1");
        request.setConfirmPassword("ValidPass1");
        request.setRole(role);
        return request;
    }

    private void verifyNoRegistrationInteractions() {
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
