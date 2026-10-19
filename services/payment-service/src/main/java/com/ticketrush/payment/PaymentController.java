package com.ticketrush.payment;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final PaymentService service;
    private final JwtVerifier jwt;

    public PaymentController(PaymentService service, JwtVerifier jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "payment");
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> pay(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                       @Valid @RequestBody PaymentRequest request) {
        var user = jwt.verify(authorization);
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header (1-100 chars) is required");
        }
        var result = service.pay(user, idempotencyKey, request);
        return ResponseEntity.status(statusFor(result)).body(result.payment());
    }

    @GetMapping("/payments")
    public List<Payment> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.list(jwt.verify(authorization));
    }

    @GetMapping("/payments/{id}")
    public Payment get(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable String id) {
        return service.get(jwt.verify(authorization), id);
    }

    private static HttpStatus statusFor(PaymentService.Result result) {
        return switch (result.payment().getStatus()) {
            case FAILED -> HttpStatus.PAYMENT_REQUIRED;
            case REFUNDED -> HttpStatus.CONFLICT;
            // CHARGED: money taken, booking confirmation still being retried
            case PROCESSING, CHARGED -> HttpStatus.ACCEPTED;
            case SUCCEEDED -> result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        };
    }
}
