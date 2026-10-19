package com.ticketrush.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Talks to booking-service's /internal API. Network errors surface as RestClientException. */
@Component
public class BookingClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Booking(
            long id,
            @JsonProperty("user_id") long userId,
            @JsonProperty("user_email") String userEmail,
            @JsonProperty("show_id") long showId,
            @JsonProperty("seat_labels") List<String> seatLabels,
            BigDecimal amount,
            String status) {
    }

    public enum ConfirmResult { CONFIRMED, NOT_PENDING }

    private final RestClient client;

    public BookingClient(RestClient.Builder builder,
                         @Value("${ticketrush.booking-url}") String bookingUrl,
                         @Value("${ticketrush.internal-token}") String internalToken) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.client = builder.baseUrl(bookingUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    public Optional<Booking> get(long bookingId) {
        try {
            return Optional.ofNullable(client.get()
                    .uri("/internal/bookings/{id}", bookingId)
                    .retrieve()
                    .body(Booking.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public ConfirmResult confirm(long bookingId, String paymentId) {
        try {
            client.post()
                    .uri("/internal/bookings/{id}/confirm", bookingId)
                    .body(Map.of("payment_id", paymentId))
                    .retrieve()
                    .toBodilessEntity();
            return ConfirmResult.CONFIRMED;
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.NotFound e) {
            return ConfirmResult.NOT_PENDING;
        }
    }
}
