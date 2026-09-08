package com.KairoLink.service;

import com.KairoLink.dto.RegistrationRequest;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.exception.DuplicateEmailException;
import com.KairoLink.exception.InvalidRegistrationException;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

@Service
public class UserRegistrationService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationRequest request) {
        if (request == null) {
            throw new InvalidRegistrationException("Registration request is required");
        }

        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        validateRole(request.getRole());
        validatePassword(password, request.getConfirmPassword());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        User user = new User();
        user.setName(normalizeRequiredText(request.getName(), "Name"));
        user.setEmail(email);
        user.setPhone(normalizePhone(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(Set.of(request.getRole()));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw new DuplicateEmailException("An account with this email already exists", exception);
            }
            throw exception;
        }
    }

    private void validateRole(Role role) {
        if (role != Role.RIDER && role != Role.DRIVER) {
            throw new InvalidRegistrationException("Public registration supports only Rider or Driver");
        }
    }

    private void validatePassword(String password, String confirmPassword) {
        if (password == null || confirmPassword == null || !password.equals(confirmPassword)) {
            throw new InvalidRegistrationException("Password and confirmation must match");
        }

        if (password.length() < 8) {
            throw new InvalidRegistrationException("Password must be at least 8 characters long");
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidRegistrationException("Password exceeds the maximum supported length");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new InvalidRegistrationException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new InvalidRegistrationException("Email is required");
        }
        return normalizedEmail;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null) {
            throw new InvalidRegistrationException(fieldName + " is required");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new InvalidRegistrationException(fieldName + " is required");
        }
        return normalizedValue;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        String normalizedPhone = phone.trim();
        return normalizedPhone.isEmpty() ? null : normalizedPhone;
    }

    private boolean isEmailUniqueConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && EMAIL_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }

            if (current instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains(EMAIL_UNIQUE_CONSTRAINT)) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }
}
