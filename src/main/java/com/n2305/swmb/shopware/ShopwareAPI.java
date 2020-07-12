package com.n2305.swmb.shopware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.n2305.swmb.properties.ShopwareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class ShopwareAPI {
    private static final Logger logger = LoggerFactory.getLogger(ShopwareAPI.class);
    private static final int LIST_LIMIT = 100;

    private final WebClient httpClient;
    private final FilterQueryParamSerializer filterQueryParamSerializer;
    private final ObjectMapper objectMapper;
    private final ShopwareProperties swProps;
    private final Clock clock;

    public ShopwareAPI(
        WebClient httpClient,
        FilterQueryParamSerializer filterParamSerializer,
        ObjectMapper objectMapper,
        ShopwareProperties swProps,
        Clock clock
    ) {
        this.httpClient = httpClient;
        this.filterQueryParamSerializer = filterParamSerializer;
        this.objectMapper = objectMapper;
        this.swProps = swProps;
        this.clock = clock;
    }

    public Mono<List<OrderListItem>> fetchOrders() {
        return fetchOrders(Collections.emptyList());
    }

    public Mono<List<OrderListItem>> fetchOrders(List<Filter> filters) {
        ObjectReader objectReader = objectMapper
            .readerForListOf(OrderListItem.class)
            .at("/data");

        logger.info("Fetch orders");

        return httpClient.get()
            .uri(uriBuilder -> {
                filterQueryParamSerializer
                    .serialize(filters)
                    .forEach(uriBuilder::queryParam);

                return uriBuilder
                    .path("/api/orders")
                    .queryParam("limit", LIST_LIMIT)
                    .queryParam("sort[0][property]", "orderTime")
                    .queryParam("sort[0][direction]", "ASC")
                    .build();
            })
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> logger.warn("Failed to fetch orders {}", e.getMessage()))
            .<List<OrderListItem>>flatMap(src -> {
                try {
                    return Mono.just(objectReader.readValue(src));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize orders", e);
                    return Mono.error(e);
                }
            });
    }

    public Mono<SWOrder> fetchOrder(int id) {
        ObjectReader objectReader = objectMapper
            .readerFor(SWOrder.class)
            .at("/data");

        logger.info("Fetch order with id {}", id);

        return httpClient.get()
            .uri(uriBuilder -> {
                return uriBuilder
                    .path("/api/orders")
                    .pathSegment("{id}")
                    .build(id);
            })
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> logger.warn("Failed to fetch order with id {}", id))
            .<SWOrder>flatMap(src -> {
                try {
                    return Mono.just(objectReader.readValue(src));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize order", e);
                    return Mono.error(e);
                }
            });
    }

    public Mono<ResponseEntity<Void>> markOrderAsExported(SWOrder order) {
        logger.info("Mark order with id {} as exported", order.getId());

        ObjectNode rootNode = objectMapper.createObjectNode()
            .set("attribute", objectMapper.createObjectNode()
                .putPOJO(swProps.getExportedAttribute(), OffsetDateTime.now(clock)));

        return httpClient.put()
            .uri(uriBuilder -> uriBuilder
                .path("/api/orders")
                .pathSegment("{id}")
                .build(order.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(rootNode))
            .retrieve()
            .toBodilessEntity()
            .doOnError(e -> logger.warn("Failed to mark order as exported", e));
    }

    public static class Filter {
        private final String property;
        private final String expression;
        private final String value;
        private final boolean operator;

        public Filter(String property, String value, String expression, Boolean operator) {
            this.property = property;
            this.value = value;
            this.expression = expression == null ? "LIKE" : expression;
            this.operator = operator == null ? false : operator.booleanValue();
        }

        public Filter(String property, String value) {
            this(property, value, null, null);
        }

        public Filter(String property, String value, String expression) {
            this(property, value, expression, null);
        }

        public String getProperty() {
            return property;
        }

        public String getExpression() {
            return expression;
        }

        public String getValue() {
            return value;
        }

        public boolean getOperator() {
            return operator;
        }
    }
}
