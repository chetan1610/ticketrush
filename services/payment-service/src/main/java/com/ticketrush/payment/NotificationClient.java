package com.ticketrush.payment;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationClient {

    private final RestClient client;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${ticketrush.notification-url}") String notificationUrl,
                              @Value("${ticketrush.internal-token}") String internalToken) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(10000);
        this.client = builder.baseUrl(notificationUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public void ticketConfirmed(BookingClient.Booking booking, Payment payment) {
        client.post()
                .uri("/internal/notifications/ticket-confirmed")
                .body(Map.of(
                        "bookingId", booking.id(),
                        "showId", booking.showId(),
                        "email", booking.userEmail(),
                        "seats", booking.seatLabels(),
                        "amount", payment.getAmount(),
                        "paymentId", payment.getId()))
                .retrieve()
                .toBodilessEntity();
    }
}
