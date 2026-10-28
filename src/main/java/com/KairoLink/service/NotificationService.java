package com.KairoLink.service;

import com.KairoLink.entity.Notification;
import com.KairoLink.entity.NotificationType;
import com.KairoLink.entity.User;
import com.KairoLink.exception.NotificationAccessDeniedException;
import com.KairoLink.exception.NotificationNotFoundException;
import com.KairoLink.exception.UserNotFoundException;
import com.KairoLink.repository.NotificationRepository;
import com.KairoLink.repository.RoundTripRequestRepository;
import com.KairoLink.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final DateTimeFormatter ROUND_TRIP_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'at' h:mm a", Locale.ENGLISH);

    private final NotificationRepository notificationRepository;
    private final RoundTripRequestRepository roundTripRequestRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            RoundTripRequestRepository roundTripRequestRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.roundTripRequestRepository = roundTripRequestRepository;
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
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiver.getId())
                .stream()
                .peek(this::formatLegacyRoundTripMessage)
                .toList();
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

    private void formatLegacyRoundTripMessage(Notification notification) {
        if (notification == null
                || notification.getBookingId() == null
                || !"Round-trip accepted".equals(notification.getTitle())
                || notification.getMessage() == null
                || !notification.getMessage().contains("T")) {
            return;
        }

        roundTripRequestRepository.findByOutboundBookingIdAndStatus(
                        notification.getBookingId(),
                        com.KairoLink.entity.RoundTripRequestStatus.ACCEPTED)
                .map(com.KairoLink.entity.RoundTripRequest::getConfirmedReturnRide)
                .map(com.KairoLink.entity.Ride::getDepartureTime)
                .map(this::acceptedRoundTripMessage)
                .ifPresent(notification::setMessage);
    }

    private String acceptedRoundTripMessage(LocalDateTime departure) {
        if (departure == null) {
            return "Your return journey has been accepted.";
        }
        return "Your return journey has been accepted for "
                + ROUND_TRIP_DATE_TIME_FORMAT.format(departure) + ".";
    }
}
