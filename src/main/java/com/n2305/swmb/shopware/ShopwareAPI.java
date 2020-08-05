package com.n2305.swmb.shopware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.n2305.swmb.properties.ShopwareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    public Mono<List<CustomerListItem>> fetchCustomers() {
        return fetchCustomers(Collections.emptyList());
    }

    public Mono<List<CustomerListItem>> fetchCustomers(List<Filter> filters) {
        ObjectReader objectReader = objectMapper
            .readerForListOf(CustomerListItem.class)
            .at("/data");

        logger.info("Fetch orders");

        return httpClient.get()
            .uri(uriBuilder -> {
                filterQueryParamSerializer
                    .serialize(filters)
                    .forEach(uriBuilder::queryParam);

                URI uri = uriBuilder
                    .path("/api/customers")
                    .queryParam("limit", LIST_LIMIT)
                    .queryParam("sort[0][property]", "id")
                    .queryParam("sort[0][direction]", "ASC")
                    .build();

                logger.info("Fetch customers with uri: {}", uri.toString());

                return uri;
            })
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> logger.warn("Failed to fetch customers {}", e.getMessage()))
            .<List<CustomerListItem>>flatMap(src -> {
                try {
                    List<CustomerListItem> result = objectReader.readValue(src);
                    logger.info("Fetched customers with ids: {}", result.stream()
                        .map(CustomerListItem::getId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));

                    OffsetDateTime now = OffsetDateTime.now(clock);
                    result.forEach(oli -> oli.setFetchTime(now));

                    return Mono.just(result);
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize customers", e);
                    return Mono.error(e);
                }
            });
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

                URI uri = uriBuilder
                    .path("/api/orders")
                    .queryParam("limit", LIST_LIMIT)
                    .queryParam("sort[0][property]", "orderTime")
                    .queryParam("sort[0][direction]", "ASC")
                    .build();

                logger.info("Fetch orders with uri: {}", uri.toString());

                return uri;
            })
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> logger.warn("Failed to fetch orders {}", e.getMessage()))
            .<List<OrderListItem>>flatMap(src -> {
                try {
                    List<OrderListItem> result = objectReader.readValue(src);
                    logger.info("Fetched orders with ids: {}", result.stream()
                        .map(OrderListItem::getId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));

                    OffsetDateTime now = OffsetDateTime.now(clock);
                    result.forEach(oli -> oli.setFetchTime(now));

                    return Mono.just(result);
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
                    SWOrder data = objectReader.readValue(src);
                    data.setFetchTime(OffsetDateTime.now(clock));

                    return Mono.just(data);
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize order", e);
                    return Mono.error(e);
                }
            });
    }

    public Mono<ResponseEntity<Void>> markOrderAsExported(SWOrder order) {
        logger.info("Mark order with id {} as exported, lt: {} t: {}",
            order.getId(), order.getListFetchTime(), order.getFetchTime());

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

        @ConstructorBinding
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
