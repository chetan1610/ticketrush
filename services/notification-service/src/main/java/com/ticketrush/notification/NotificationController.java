package com.ticketrush.notification;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "notification");
    }

    @PostMapping("/internal/notifications/ticket-confirmed")
    public Notification ticketConfirmed(@Valid @RequestBody TicketConfirmedRequest request) {
        return service.ticketConfirmed(request);
    }

    @GetMapping("/internal/notifications")
    public List<Notification> list(@RequestParam String email) {
        return service.forEmail(email);
    }

}
