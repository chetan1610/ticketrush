package com.ticketrush.payment;

import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Deliberately not @Transactional: each save commits on its own, so we never hold a
 * DB transaction open across the card gateway or another service's HTTP call.
 */
@Service
public class PaymentService {

    public record Result(Payment payment, boolean replayed) {
    }

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final BookingClient bookings;
    private final NotificationClient notifications;
    private final PaymentGateway gateway;

    public PaymentService(PaymentRepository repository, BookingClient bookings,
                          NotificationClient notifications, PaymentGateway gateway) {
        this.repository = repository;
        this.bookings = bookings;
        this.notifications = notifications;
        this.gateway = gateway;
    }

    public Result pay(AuthUser user, String idempotencyKey, PaymentRequest request) {
        var previous = repository.findByUserIdAndIdempotencyKey(user.id(), idempotencyKey);
        if (previous.isPresent()) {
            return new Result(previous.get(), true);
        }

        var booking = loadBooking(request.bookingId())
                .filter(b -> b.userId() == user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "booking not found"));
        if (!"PENDING".equals(booking.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "booking is " + booking.status() + ", cannot pay");
        }

        Payment payment;
        try {
            payment = repository.saveAndFlush(
                    new Payment(user.id(), booking.id(), idempotencyKey, booking.amount()));
        } catch (DataIntegrityViolationException e) {
            // Either the same key raced us, or another payment for this booking is live.
            return repository.findByUserIdAndIdempotencyKey(user.id(), idempotencyKey)
                    .map(p -> new Result(p, true))
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "booking already has a payment in progress"));
        }

        var declined = gateway.charge(request.cardNumber(), payment.getAmount());
        if (declined.isPresent()) {
            payment.fail(declined.get());
            return new Result(repository.save(payment), false);
        }
        payment.charged();
        payment = repository.save(payment);

        return new Result(complete(payment, booking), false);
    }

    public Payment get(AuthUser user, String paymentId) {
        return repository.findById(paymentId)
                .filter(p -> p.getUserId() == user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "payment not found"));
    }

    public List<Payment> list(AuthUser user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.id());
    }

    /**
     * Money is taken but booking-service or notification-service didn't answer.
     * Keep retrying; both downstream calls are idempotent, so retries are safe.
     */
    @Scheduled(fixedDelayString = "${payment.reconcile-delay-ms:30000}")
    public void reconcile() {
        var cutoff = OffsetDateTime.now().minusSeconds(30);
        for (var payment : repository.findTop50ByStatusAndUpdatedAtBeforeOrderByCreatedAt(
                Payment.Status.CHARGED, cutoff)) {
            loadBookingQuietly(payment.getBookingId()).ifPresent(b -> complete(payment, b));
        }
        for (var payment : repository.findTop50ByStatusAndNotifiedFalseAndUpdatedAtBeforeOrderByCreatedAt(
                Payment.Status.SUCCEEDED, cutoff)) {
            loadBookingQuietly(payment.getBookingId()).ifPresent(b -> notify(payment, b));
        }
    }

    private Payment complete(Payment payment, BookingClient.Booking booking) {
        try {
            if (bookings.confirm(booking.id(), payment.getId()) == BookingClient.ConfirmResult.NOT_PENDING) {
                gateway.refund(payment.getId(), payment.getAmount());
                payment.refunded("booking expired or was cancelled before payment completed");
                return repository.save(payment);
            }
        } catch (RestClientException e) {
            log.warn("confirm for booking {} failed, will retry: {}", booking.id(), e.getMessage());
            return payment;
        }
        payment.succeeded();
        payment = repository.save(payment);
        return notify(payment, booking);
    }

    private Payment notify(Payment payment, BookingClient.Booking booking) {
        try {
            notifications.ticketConfirmed(booking, payment);
        } catch (RestClientException e) {
            log.warn("notification for booking {} failed, will retry: {}", booking.id(), e.getMessage());
            return payment;
        }
        payment.markNotified();
        return repository.save(payment);
    }

    private java.util.Optional<BookingClient.Booking> loadBooking(long bookingId) {
        try {
            return bookings.get(bookingId);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "booking service unavailable");
        }
    }

    private java.util.Optional<BookingClient.Booking> loadBookingQuietly(long bookingId) {
        try {
            return bookings.get(bookingId);
        } catch (RestClientException e) {
            return java.util.Optional.empty();
        }
    }
}
