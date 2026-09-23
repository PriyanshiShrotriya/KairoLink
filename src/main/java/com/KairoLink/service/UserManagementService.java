package com.KairoLink.service;

import com.KairoLink.entity.User;
import com.KairoLink.entity.UserStatus;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public User suspendUser(Long userId, String adminEmail) {
        User admin = findAdmin(adminEmail);

        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot suspend your own account");
        }

        User user = getUserById(userId);
        user.setStatus(UserStatus.SUSPENDED);
        return userRepository.save(user);
    }

    @Transactional
    public User banUser(Long userId, String adminEmail) {
        User admin = findAdmin(adminEmail);

        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot ban your own account");
        }

        User user = getUserById(userId);
        user.setStatus(UserStatus.BANNED);
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(Long userId, String adminEmail) {
        User admin = findAdmin(adminEmail);

        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot reactivate your own account");
        }

        User user = getUserById(userId);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private User findAdmin(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Admin email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
