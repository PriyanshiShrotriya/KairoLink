package com.KairoLink.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/admin";
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DRIVER".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/driver";
        }
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RIDER".equals(authority.getAuthority()))) {
            return "redirect:/dashboard/rider";
        }
        throw new AccessDeniedException("No dashboard role is assigned");
    }

    @GetMapping("/dashboard/rider")
    public String riderDashboard() {
        return "dashboard/rider";
    }

    @GetMapping("/dashboard/driver")
    public String driverDashboard() {
        return "dashboard/driver";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard() {
        return "dashboard/admin";
    }
}
