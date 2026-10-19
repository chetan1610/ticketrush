package com.ticketrush.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotNull Long bookingId,
        @NotNull @Pattern(regexp = "\\d{12,19}", message = "must be 12-19 digits") String cardNumber) {
}
