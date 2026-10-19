package com.ticketrush.notification;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final RestClient client;

    public CatalogClient(@Value("${ticketrush.catalog-url}") String catalogUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl(catalogUrl).requestFactory(factory).build();
    }

    /** Show details make the email nicer, but the ticket still goes out without them. */
    public Optional<Map<String, Object>> getShow(long showId) {
        try {
            return Optional.ofNullable(client.get()
                    .uri("/shows/{id}", showId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (RestClientException e) {
            log.warn("could not load show {} from catalog: {}", showId, e.getMessage());
            return Optional.empty();
        }
    }
}
