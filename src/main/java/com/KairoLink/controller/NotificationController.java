package com.KairoLink.controller;

import com.KairoLink.entity.Notification;
import com.KairoLink.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listNotifications(Authentication authentication, Model model) {
        String email = authentication.getName();
        List<Notification> notifications = notificationService.getNotifications(email);
        long unreadCount = notificationService.getUnreadCount(email);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(Authentication authentication, @PathVariable Long id) {
        notificationService.markAsRead(authentication.getName(), id);
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return "redirect:/notifications";
    }
}
