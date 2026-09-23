package com.KairoLink.service;

import com.KairoLink.entity.Notification;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.User;
import com.KairoLink.exception.NotificationAccessDeniedException;
import com.KairoLink.exception.NotificationNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.NotificationRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(
            String receiverEmail,
            NotificationType type,
            String title,
            String message) {
        return createNotification(receiverEmail, type, title, message, null);
    }

    @Transactional
    public Notification createNotification(
            String receiverEmail,
            NotificationType type,
            String title,
            String message,
            Long bookingId) {
        User receiver = findUser(receiverEmail);
        validateContent(type, title, message);

        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setType(type);
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setRead(false);
        notification.setBookingId(bookingId);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(String receiverEmail) {
        User receiver = findUser(receiverEmail);
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiver.getId());
    }

    public List<Notification> getUnreadNotifications(String receiverEmail) {
        User receiver = findUser(receiverEmail);
        return notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(receiver.getId());
    }

    public long getUnreadCount(String receiverEmail) {
        User receiver = findUser(receiverEmail);
        return notificationRepository.countByReceiverIdAndReadFalse(receiver.getId());
    }

    @Transactional
    public Notification markAsRead(String receiverEmail, Long notificationId) {
        User receiver = findUser(receiverEmail);
        Notification notification = findNotification(notificationId);
        validateOwnership(notification, receiver);

        if (!notification.isRead()) {
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return notification;
    }

    @Transactional
    public int markAllAsRead(String receiverEmail) {
        User receiver = findUser(receiverEmail);
        List<Notification> unreadNotifications =
                notificationRepository.findByReceiverIdAndReadFalseOrderByCreatedAtDesc(receiver.getId());

        unreadNotifications.forEach(notification -> notification.setRead(true));
        if (!unreadNotifications.isEmpty()) {
            notificationRepository.saveAll(unreadNotifications);
        }
        return unreadNotifications.size();
    }

    private User findUser(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("Notification receiver was not found");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Notification receiver was not found"));
    }

    private Notification findNotification(Long notificationId) {
        if (notificationId == null) {
            throw new NotificationNotFoundException("Notification was not found");
        }
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification was not found"));
    }

    private void validateOwnership(Notification notification, User receiver) {
        if (notification.getReceiver() == null
                || notification.getReceiver().getId() == null
                || !notification.getReceiver().getId().equals(receiver.getId())) {
            throw new NotificationAccessDeniedException("Notification does not belong to the authenticated user");
        }
    }

    private void validateContent(
            NotificationType type,
            String title,
            String message) {
        if (type == null) {
            throw new IllegalArgumentException("Notification type is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Notification message is required");
        }
    }
}
