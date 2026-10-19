package com.ticketrush.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByBookingId(Long bookingId);

    List<Notification> findByEmailOrderByIdDesc(String email);

    List<Notification> findTop50ByStatusAndAttemptsLessThanOrderById(Notification.Status status, int maxAttempts);
}
