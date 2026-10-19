package com.ticketrush.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum Status { PENDING, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long bookingId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private int attempts;
    private String lastError;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime sentAt;

    protected Notification() {
    }

    public Notification(Long bookingId, String email, String subject, String body) {
        this.bookingId = bookingId;
        this.email = email;
        this.subject = subject;
        this.body = body;
    }

    public void markSent() {
        attempts++;
        status = Status.SENT;
        lastError = null;
        sentAt = OffsetDateTime.now();
    }

    public void markFailed(String error) {
        attempts++;
        status = Status.FAILED;
        lastError = error;
    }

    public Long getId() { return id; }
    public Long getBookingId() { return bookingId; }
    public String getEmail() { return email; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getSentAt() { return sentAt; }
}
