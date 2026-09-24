package com.KairoLink.config;

import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NavbarDataControllerAdviceTest {

    @Test
    void defaultsUnreadCountWhenAuthenticatedUserIsNotPersisted() {
        NotificationService notificationService = mock(NotificationService.class);
        when(notificationService.getUnreadCount("rider"))
                .thenThrow(new UserNotFoundException("Notification receiver was not found"));

        ExtendedModelMap model = new ExtendedModelMap();
        new NavbarDataControllerAdvice(notificationService)
                .addNotificationCount(model,
                        UsernamePasswordAuthenticationToken.authenticated(
                                "rider", "password", java.util.List.of()));

        assertEquals(0L, model.getAttribute("unreadCount"));
    }

    @Test
    void preservesUnreadCountForPersistedUser() {
        NotificationService notificationService = mock(NotificationService.class);
        when(notificationService.getUnreadCount("rider@example.com")).thenReturn(3L);

        ExtendedModelMap model = new ExtendedModelMap();
        new NavbarDataControllerAdvice(notificationService)
                .addNotificationCount(model,
                        UsernamePasswordAuthenticationToken.authenticated(
                                "rider@example.com", "password", java.util.List.of()));

        assertEquals(3L, model.getAttribute("unreadCount"));
    }
}
