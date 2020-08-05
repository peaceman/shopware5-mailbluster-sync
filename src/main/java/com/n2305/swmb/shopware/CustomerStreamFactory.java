package com.n2305.swmb.shopware;

import com.n2305.swmb.properties.ShopwareProperties;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
public class CustomerStreamFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomerStreamFactory.class);

    private final ShopwareAPI shopwareAPI;
    private final ShopwareProperties swProps;
    private final EmailValidator emailValidator;

    public CustomerStreamFactory(ShopwareAPI shopwareAPI, ShopwareProperties swProps, EmailValidator emailValidator) {
        this.shopwareAPI = shopwareAPI;
        this.swProps = swProps;
        this.emailValidator = emailValidator;
    }

    public Flux<CustomerListItem> create() throws IOException {
        CustomerPublisher customerPublisher = new CustomerPublisher(shopwareAPI, swProps);

        return Flux.create(customerPublisher)
            .filter(cli -> emailValidator.isValid(cli.getEmail()));
    }
}
