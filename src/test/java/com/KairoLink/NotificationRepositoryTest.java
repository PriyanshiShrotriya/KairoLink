package com.KairoLink;

import com.KairoLink.entity.Notification;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.Role;
import com.KairoLink.entity.User;
import com.KairoLink.repository.NotificationRepository;
import com.KairoLink.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void savesAndRetrievesNotificationByReceiver() {
        User receiver = saveUser("notification-receiver@example.com");

        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setType(NotificationType.BOOKING_ACCEPTED);
        notification.setTitle("Booking accepted");
        notification.setMessage("Your booking has been accepted.");

        Notification saved = notificationRepository.saveAndFlush(notification);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.isRead());
        assertEquals(NotificationType.BOOKING_ACCEPTED, saved.getType());

        List<Notification> notifications =
                notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiver.getId());
        assertEquals(1, notifications.size());
        assertEquals(saved.getId(), notifications.get(0).getId());
    }

    @Test
    @Transactional
    void retrievesUnreadNotificationsAndCountsThem() {
        User receiver = saveUser("notification-unread@example.com");

        Notification unread = notification(receiver, NotificationType.RIDE_REMINDER, false);
        Notification read = notification(receiver, NotificationType.GENERAL, true);
        notificationRepository.saveAndFlush(unread);
        notificationRepository.saveAndFlush(read);

        List<Notification> unreadNotifications =
                notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(receiver.getId());

        assertEquals(1, unreadNotifications.size());
        assertEquals(NotificationType.RIDE_REMINDER, unreadNotifications.get(0).getType());
        assertEquals(1, notificationRepository.countByReceiverIdAndReadFalse(receiver.getId()));
        assertTrue(unreadNotifications.get(0).isRead() == false);
    }

    private Notification notification(User receiver, NotificationType type, boolean read) {
        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setType(type);
        notification.setTitle(type.name());
        notification.setMessage("Notification message");
        notification.setRead(read);
        return notification;
    }

    private User saveUser(String email) {
        User user = new User();
        user.setName("Notification Test User");
        user.setEmail(email);
        user.setPasswordHash("hashedpassword");
        user.setRoles(Set.of(Role.RIDER));
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }
}
