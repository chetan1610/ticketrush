package com.ticketrush.notification;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h:mm a");

    private final NotificationRepository repository;
    private final CatalogClient catalog;
    private final JavaMailSender mailSender;
    private final String from;

    public NotificationService(NotificationRepository repository, CatalogClient catalog,
                               JavaMailSender mailSender, @Value("${notification.from}") String from) {
        this.repository = repository;
        this.catalog = catalog;
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Idempotent per booking: payment-service may call us more than once. */
    public Notification ticketConfirmed(TicketConfirmedRequest req) {
        var existing = repository.findByBookingId(req.bookingId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Notification notification;
        try {
            notification = repository.save(new Notification(
                    req.bookingId(), req.email(), "Your TicketRush booking #" + req.bookingId(), render(req)));
        } catch (DataIntegrityViolationException e) {
            return repository.findByBookingId(req.bookingId()).orElseThrow();
        }
        return send(notification);
    }

    public List<Notification> forEmail(String email) {
        return repository.findByEmailOrderByIdDesc(email.toLowerCase());
    }

    @Scheduled(fixedDelayString = "${notification.retry-delay-ms:60000}")
    public void retryFailed() {
        for (var notification : repository.findTop50ByStatusAndAttemptsLessThanOrderById(
                Notification.Status.FAILED, MAX_ATTEMPTS)) {
            send(notification);
        }
    }

    private Notification send(Notification notification) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.getEmail());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());
        try {
            mailSender.send(message);
            notification.markSent();
            log.info("sent ticket email for booking {} to {}", notification.getBookingId(), notification.getEmail());
        } catch (MailException e) {
            notification.markFailed(e.getMessage());
            log.warn("email for booking {} failed (attempt {}): {}",
                    notification.getBookingId(), notification.getAttempts(), e.getMessage());
        }
        return repository.save(notification);
    }

    private String render(TicketConfirmedRequest req) {
        var show = catalog.getShow(req.showId());
        var lines = new StringBuilder("Your booking is confirmed!\n\n");
        lines.append("Booking:  #").append(req.bookingId()).append('\n');
        show.ifPresentOrElse(s -> appendShow(lines, s),
                () -> lines.append("Show:     #").append(req.showId()).append('\n'));
        lines.append("Seats:    ").append(String.join(", ", req.seats())).append('\n');
        lines.append("Paid:     Rs. ").append(req.amount()).append(" (payment ").append(req.paymentId()).append(")\n\n");
        lines.append("Show this email at the counter. Enjoy the movie!\n- TicketRush");
        return lines.toString();
    }

    private static void appendShow(StringBuilder lines, Map<String, Object> show) {
        var startsAt = OffsetDateTime.parse((String) show.get("starts_at")).atZoneSameInstant(IST);
        lines.append("Movie:    ").append(show.get("movie_title")).append(" (").append(show.get("language")).append(")\n");
        lines.append("Venue:    ").append(show.get("venue")).append(", ").append(show.get("city"))
                .append(" - ").append(show.get("screen")).append('\n');
        lines.append("When:     ").append(WHEN.format(startsAt)).append(" IST\n");
    }
}
