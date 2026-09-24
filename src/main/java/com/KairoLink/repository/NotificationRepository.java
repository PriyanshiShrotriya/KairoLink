package com.KairoLink.repository;

import com.KairoLink.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<Notification> findByReceiverIdAndReadFalseOrderByCreatedAtDesc(Long receiverId);

    long countByReceiverIdAndReadFalse(Long receiverId);
}
