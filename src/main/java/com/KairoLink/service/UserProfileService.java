package com.KairoLink.service;

import com.KairoLink.dto.ProfileUpdateRequest;
import com.KairoLink.dto.ProfileView;
import com.KairoLink.entity.User;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ProfileView getProfile(String email) {
        return toProfileView(findUser(email));
    }

    @Transactional
    public ProfileView updateProfile(String email, ProfileUpdateRequest request) {
        User user = findUser(email);
        user.setName(request.getName().trim());
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setProfilePhotoReference(normalizeOptional(request.getProfilePhotoReference()));
        return toProfileView(userRepository.save(user));
    }

    private User findUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user was not found"));
    }

    private ProfileView toProfileView(User user) {
        return new ProfileView(
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfilePhotoReference(),
                user.getRoles(),
                user.isEnabled(),
                user.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Authenticated user was not found");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
