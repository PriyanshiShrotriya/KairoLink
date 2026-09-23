package com.KairoLink.config;

import com.KairoLink.exception.UserNotFoundException;
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
            long unreadCount;
            try {
                unreadCount = notificationService.getUnreadCount(authentication.getName());
            } catch (UserNotFoundException exception) {
                unreadCount = 0;
            }
            model.addAttribute("unreadCount", unreadCount);
        }
    }
}