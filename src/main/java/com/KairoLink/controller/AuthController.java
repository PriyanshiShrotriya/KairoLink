package com.KairoLink.controller;

import com.KairoLink.dto.RegistrationRequest;
import com.KairoLink.exception.DuplicateEmailException;
import com.KairoLink.exception.InvalidRegistrationException;
import com.KairoLink.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;

@Controller
public class AuthController {

    private final UserRegistrationService userRegistrationService;

    public AuthController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest registrationRequest,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            clearPasswords(registrationRequest);
            return "auth/register";
        }

        try {
            userRegistrationService.register(registrationRequest);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "registration.email.duplicate", exception.getMessage());
            clearPasswords(registrationRequest);
            return "auth/register";
        } catch (InvalidRegistrationException exception) {
            bindingResult.reject("registration.invalid", exception.getMessage());
            clearPasswords(registrationRequest);
            return "auth/register";
        }

        return "redirect:/register?success";
    }

    private void clearPasswords(RegistrationRequest registrationRequest) {
        registrationRequest.setPassword(null);
        registrationRequest.setConfirmPassword(null);
    }
}
