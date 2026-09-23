package com.KairoLink.config;

import com.KairoLink.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavbarDataControllerAdvice {

    private final NotificationService notificationService;

    public NavbarDataControllerAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute
    public void addNotificationCount(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            long unreadCount = notificationService.getUnreadCount(authentication.getName());
            model.addAttribute("unreadCount", unreadCount);
        }
    }
}