package com.ticketrush.payment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findTop50ByStatusAndUpdatedAtBeforeOrderByCreatedAt(Payment.Status status, OffsetDateTime before);

    List<Payment> findTop50ByStatusAndNotifiedFalseAndUpdatedAtBeforeOrderByCreatedAt(
            Payment.Status status, OffsetDateTime before);
}
