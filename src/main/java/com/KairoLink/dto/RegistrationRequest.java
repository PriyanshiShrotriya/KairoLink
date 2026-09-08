package com.KairoLink.dto;

import com.KairoLink.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String phone;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    @Size(min = 8)
    private String confirmPassword;

    @NotNull
    private Role role;
}
