package com.ticketrush.payment;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Fake card processor. Stands in for Razorpay/Stripe so the flow can be run locally.
 * Cards ending in 0002 are declined, like the test cards real gateways hand out.
 */
@Component
public class PaymentGateway {

    /** Returns a decline reason, or empty if the charge went through. */
    public Optional<String> charge(String cardNumber, BigDecimal amount) {
        if (cardNumber.endsWith("0002")) {
            return Optional.of("card declined");
        }
        return Optional.empty();
    }

    public void refund(String paymentId, BigDecimal amount) {
        // a real gateway call goes here
    }
}
