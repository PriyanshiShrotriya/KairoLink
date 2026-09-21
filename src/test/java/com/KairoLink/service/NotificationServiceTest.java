package com.KairoLink.service;

import com.KairoLink.entity.Notification;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.User;
import com.KairoLink.exception.NotificationAccessDeniedException;
import com.KairoLink.exception.NotificationNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.NotificationRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = new NotificationService(notificationRepository, userRepository);
    }

    @Test
    void createsUnreadNotificationForNormalizedReceiverEmail() {
        User receiver = user(1L, "receiver@example.com");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService.createNotification(
                " RECEIVER@EXAMPLE.COM ",
                NotificationType.GENERAL,
                "  Hello  ",
                "  A message  ");

        assertEquals(receiver, notification.getReceiver());
        assertEquals(NotificationType.GENERAL, notification.getType());
        assertEquals("Hello", notification.getTitle());
        assertEquals("A message", notification.getMessage());
        assertFalse(notification.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void retrievesAllUnreadAndUnreadCountForReceiver() {
        User receiver = user(1L, "receiver@example.com");
        Notification unread = notification(10L, receiver, false);
        Notification read = notification(11L, receiver, true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(unread, read));
        when(notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(unread));
        when(notificationRepository.countByReceiverIdAndReadFalse(1L)).thenReturn(1L);

        assertEquals(2, notificationService.getNotifications("receiver@example.com").size());
        assertEquals(List.of(unread), notificationService.getUnreadNotifications("receiver@example.com"));
        assertEquals(1L, notificationService.getUnreadCount("receiver@example.com"));
    }

    @Test
    void marksOwnedUnreadNotificationAsRead() {
        User receiver = user(1L, "receiver@example.com");
        Notification notification = notification(10L, receiver, false);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Notification marked = notificationService.markAsRead("receiver@example.com", 10L);

        assertTrue(marked.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void doesNotSaveAlreadyReadNotification() {
        User receiver = user(1L, "receiver@example.com");
        Notification notification = notification(10L, receiver, true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead("receiver@example.com", 10L);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void marksAllOwnedUnreadNotificationsAsRead() {
        User receiver = user(1L, "receiver@example.com");
        Notification first = notification(10L, receiver, false);
        Notification second = notification(11L, receiver, false);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(first, second));

        int updated = notificationService.markAllAsRead("receiver@example.com");

        assertEquals(2, updated);
        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(notificationRepository).saveAll(List.of(first, second));
    }

    @Test
    void rejectsUnknownReceiver() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> notificationService.getUnreadCount("missing@example.com"));
    }

    @Test
    void rejectsUnknownNotification() {
        User receiver = user(1L, "receiver@example.com");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.markAsRead("receiver@example.com", 99L));
    }

    @Test
    void rejectsModificationOfAnotherUsersNotification() {
        User receiver = user(1L, "receiver@example.com");
        User otherUser = user(2L, "other@example.com");
        Notification notification = notification(10L, otherUser, false);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(NotificationAccessDeniedException.class,
                () -> notificationService.markAsRead("receiver@example.com", 10L));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void rejectsBlankNotificationContent() {
        User receiver = user(1L, "receiver@example.com");
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));

        assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(
                        receiver.getEmail(), NotificationType.GENERAL, " ", "message"));
        assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(
                        receiver.getEmail(), NotificationType.GENERAL, "title", " "));
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Notification notification(Long id, User receiver, boolean read) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setReceiver(receiver);
        notification.setType(NotificationType.GENERAL);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setRead(read);
        return notification;
    }
}
