package com.ticketrush.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    /**
     * PROCESSING -> FAILED                 card declined
     * PROCESSING -> CHARGED -> SUCCEEDED   booking confirmed
     * PROCESSING -> CHARGED -> REFUNDED    booking expired/cancelled meanwhile
     */
    public enum Status { PROCESSING, FAILED, CHARGED, SUCCEEDED, REFUNDED }

    @Id
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PROCESSING;

    private String failureReason;

    private boolean notified;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = createdAt;

    protected Payment() {
    }

    public Payment(Long userId, Long bookingId, String idempotencyKey, BigDecimal amount) {
        this.id = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.userId = userId;
        this.bookingId = bookingId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
    }

    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public void fail(String reason) {
        status = Status.FAILED;
        failureReason = reason;
    }

    public void charged() {
        status = Status.CHARGED;
    }

    public void succeeded() {
        status = Status.SUCCEEDED;
    }

    public void refunded(String reason) {
        status = Status.REFUNDED;
        failureReason = reason;
    }

    public void markNotified() {
        notified = true;
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public boolean isNotified() { return notified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
