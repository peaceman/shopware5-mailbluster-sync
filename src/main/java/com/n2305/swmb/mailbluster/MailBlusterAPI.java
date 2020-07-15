package com.n2305.swmb.mailbluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

public class MailBlusterAPI {
    private static final Logger logger = LoggerFactory.getLogger(MailBlusterAPI.class);

    private final WebClient httpClient;
    private final ObjectMapper objectMapper;

    public MailBlusterAPI(WebClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public Mono<ResponseEntity<Void>> createOrder(MBOrder order) {
        try {
            String jsonOrder = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(order);

            return httpClient
                .post()
                .uri(uriBuilder -> uriBuilder.path("/api/orders").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(jsonOrder))
                .retrieve()
                .toBodilessEntity()
                .doOnError(
                    e -> !(e instanceof WebClientResponseException),
                    e -> logger.warn("Failed to create order", e)
                )
                .doOnError(WebClientResponseException.class, e -> {
                    logger.warn(
                        "Received WebClientResponseException for order {}:\nRequest:\n{}\nResponse:\n{}",
                        order.getId(),
                        jsonOrder,
                        String.format("%s\n%s", e.getHeaders().toString(), e.getResponseBodyAsString())
                    );
                });
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
