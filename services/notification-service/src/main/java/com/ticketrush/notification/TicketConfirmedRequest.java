package com.ticketrush.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record TicketConfirmedRequest(
        @NotNull Long bookingId,
        @NotNull Long showId,
        @NotBlank @Email String email,
        @NotEmpty List<String> seats,
        @NotNull BigDecimal amount,
        @NotBlank String paymentId) {
}
