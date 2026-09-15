package com.KairoLink.controller;

import com.KairoLink.dto.ProfileUpdateRequest;
import com.KairoLink.dto.ProfileView;
import com.KairoLink.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public String viewProfile(Authentication authentication, Model model) {
        model.addAttribute("profile", userProfileService.getProfile(authentication.getName()));
        return "profile/view";
    }

    @GetMapping("/profile/edit")
    public String showEditForm(Authentication authentication, Model model) {
        ProfileView profile = userProfileService.getProfile(authentication.getName());
        model.addAttribute("profileUpdateRequest", toUpdateRequest(profile));
        model.addAttribute("profile", profile);
        return "profile/edit";
    }

    @PostMapping("/profile")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("profile", userProfileService.getProfile(authentication.getName()));
            return "profile/edit";
        }

        userProfileService.updateProfile(authentication.getName(), request);
        return "redirect:/profile?updated";
    }

    private ProfileUpdateRequest toUpdateRequest(ProfileView profile) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(profile.getName());
        request.setPhone(profile.getPhone());
        request.setProfilePhotoReference(profile.getProfilePhotoReference());
        return request;
    }
}
