package com.KairoLink.dto;

import com.KairoLink.entity.Role;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
public class ProfileView {

    private final String name;
    private final String email;
    private final String phone;
    private final String profilePhotoReference;
    private final Set<Role> roles;
    private final boolean enabled;
    private final Instant updatedAt;

    public ProfileView(String name, String email, String phone, String profilePhotoReference,
                       Set<Role> roles, boolean enabled, Instant updatedAt) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.profilePhotoReference = profilePhotoReference;
        this.roles = Set.copyOf(roles);
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }
}
