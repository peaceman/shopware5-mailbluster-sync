package com.n2305.swmb.shopware;

import com.n2305.swmb.properties.ShopwareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderStreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(OrderStreamFactory.class);

    private final ShopwareAPI shopwareAPI;
    private final ShopwareProperties swProps;

    public OrderStreamFactory(ShopwareAPI shopwareAPI, ShopwareProperties swProps) {
        this.shopwareAPI = shopwareAPI;
        this.swProps = swProps;
    }

    public Flux<SWOrder> create() {
        OrderPublisher orderPublisher = new OrderPublisher(shopwareAPI, swProps);

        return Flux.create(orderPublisher)
            .flatMap(this::fetchOrder);
    }

    private Mono<SWOrder> fetchOrder(OrderListItem oli) {
        return shopwareAPI.fetchOrder(oli.getId())
            .onErrorResume(e -> Mono.empty());
    }
}
