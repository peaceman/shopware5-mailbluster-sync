package com.n2305.swmb.shopware;

import com.n2305.swmb.properties.ShopwareProperties;
import org.apache.commons.validator.routines.EmailValidator;
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
    private final EmailValidator emailValidator;

    public OrderStreamFactory(ShopwareAPI shopwareAPI, ShopwareProperties swProps, EmailValidator emailValidator) {
        this.shopwareAPI = shopwareAPI;
        this.swProps = swProps;
        this.emailValidator = emailValidator;
    }

    public Flux<SWOrder> create() {
        OrderPublisher orderPublisher = new OrderPublisher(shopwareAPI, swProps);

        return Flux.create(orderPublisher)
            .flatMap(this::fetchOrder)
            .filter(swo -> emailValidator.isValid(swo.getCustomer().getEmail()));
    }

    private Mono<SWOrder> fetchOrder(OrderListItem oli) {
        return shopwareAPI.fetchOrder(oli.getId())
            .map(o -> o.setListFetchTime(oli.getFetchTime()))
            .onErrorResume(e -> Mono.empty());
    }
}
