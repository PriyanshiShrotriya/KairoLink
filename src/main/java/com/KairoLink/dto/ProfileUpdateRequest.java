package com.KairoLink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 30)
    private String phone;

    @Size(max = 255)
    private String profilePhotoReference;
}
