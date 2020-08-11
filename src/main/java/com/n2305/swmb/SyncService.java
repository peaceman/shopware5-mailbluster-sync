package com.n2305.swmb;

import com.n2305.swmb.mailbluster.MBLead;
import com.n2305.swmb.mailbluster.MBOrder;
import com.n2305.swmb.mailbluster.MailBlusterAPI;
import com.n2305.swmb.mailbluster.PartnerCampaignIDMapper;
import com.n2305.swmb.properties.MailBlusterProperties;
import com.n2305.swmb.shopware.*;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Profile("!test")
public class SyncService implements DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);

    private final OrderStreamFactory orderStreamFactory;
    private final CustomerStreamFactory customerStreamFactory;
    private final MailBlusterProperties mbProps;
    private final MailBlusterAPI mbAPI;
    private final ShopwareAPI swAPI;
    private final PartnerCampaignIDMapper partnerCampaignIDMapper;
    private Disposable orderStreamDisposable;
    private Disposable customerStreamDisposable;

    public SyncService(
        OrderStreamFactory orderStreamFactory,
        CustomerStreamFactory customerStreamFactory,
        MailBlusterProperties mbProps,
        MailBlusterAPI mbAPI,
        ShopwareAPI swAPI,
        PartnerCampaignIDMapper partnerCampaignIDMapper
    ) {
        this.orderStreamFactory = orderStreamFactory;
        this.customerStreamFactory = customerStreamFactory;
        this.mbProps = mbProps;
        this.mbAPI = mbAPI;
        this.swAPI = swAPI;
        this.partnerCampaignIDMapper = partnerCampaignIDMapper;
    }

    @PostConstruct
    private void start() throws IOException {
        logger.info("Before stream start");

        RateLimiterConfig rlc = RateLimiterConfig.custom()
            .limitForPeriod(mbProps.getRequestsPerMinute())
            .limitRefreshPeriod(Duration.ofSeconds(65))
            .timeoutDuration(Duration.ZERO)
            .build();

        RateLimiter rl = RateLimiter.of("mailbluster", rlc);

        orderStreamDisposable = startOrderStream(rl);
        customerStreamDisposable = startCustomerStream(rl);

        logger.info("After stream start");
    }

    private Disposable startOrderStream(RateLimiter rl) {
        Flux<SWOrder> orderStream = orderStreamFactory.create();

        return orderStream
            .map(OrderStreamElement::new)
            .flatMap(ose -> {
                try {
                    ose.setMbOrder(mapOrder(ose.getSwOrder()));
                    return Mono.just(ose);
                } catch (Throwable e) {
                    return Mono.empty();
                }
            })
            .transform(RateLimitElements.with(rl))
            .delayElements(Duration.ofMillis(60 * 1000 / mbProps.getRequestsPerMinute()))
            .flatMap(ose -> this.mbAPI.createOrder(ose.getMbOrder())
                .thenReturn(ose)
                .onErrorResume(
                    e -> e instanceof WebClientResponseException.UnprocessableEntity
                        && ((WebClientResponseException.UnprocessableEntity) e).getResponseBodyAsString()
                        .contains("Order id already exists"),
                    e -> Mono.just(ose)
                )
                .onErrorResume(e -> Mono.empty()))
            .flatMap(ose -> this.swAPI.markOrderAsExported(ose.getSwOrder())
                .thenReturn(ose)
                .onErrorResume(e -> Mono.empty()))
            .subscribe(ose -> {
                SWOrder swOrder = ose.getSwOrder();
                logger.info("Finished handling order id: {} number: {}", swOrder.getId(), swOrder.getNumber());
            });
    }

    private MBOrder mapOrder(SWOrder swOrder) {
        SWOrder.Customer swCustomer = swOrder.getCustomer();
        MBOrder.Customer mbCustomer = new MBOrder.Customer.Builder()
            .withEmail(swCustomer.getEmail())
            .withFirstName(swCustomer.getFirstname())
            .withLastName(swCustomer.getLastname())
            .withIpAddress(swOrder.getRemoteAddress())
            .withMeta(buildCustomerMeta(swOrder))
            .build();

        List<MBOrder.Product> items = mapOrderDetails(swOrder.getDetails());
        if (swOrder.hasInvoiceShipping())
            items.add(MBOrder.Product.forShipping(swOrder.getInvoiceShipping()));

        return new MBOrder.Builder()
            .withId(swOrder.getNumber())
            .withCustomer(mbCustomer)
            .withCampaignId(partnerCampaignIDMapper.apply(swOrder.getPartnerID()))
            .withCurrency(swOrder.getCurrency())
            .withTotalPrice(swOrder.getInvoiceAmount())
            .withItems(items)
            .build();
    }

    private Map<String, String> buildCustomerMeta(SWOrder order) {
        Map<String, String> meta = new HashMap<>();

        Supplier<Stream<Optional<SWOrder.Address>>> addressStream = () ->
            Stream.of(order.getBilling(), order.getShipping())
                .map(Optional::ofNullable);

        Map<String, Function<Optional<SWOrder.Address>, String>> props = Map.of(
            "zip", oa -> oa.map(SWOrder.Address::getZipCode).orElse(""),
            "city", oa -> oa.map(SWOrder.Address::getCity).orElse(""),
            "country", oa -> oa.map(SWOrder.Address::getCountry)
                .map(SWOrder.Address.Country::getIso).orElse("")
        );

        props.forEach((key, fetchFn) -> {
            fetchFirstNonEmptyValueFromAddresses(addressStream.get(), fetchFn)
                .ifPresent(v -> meta.put(key, v));
        });

        meta.put("referer", order.getReferer());

        return meta;
    }

    private Optional<String> fetchFirstNonEmptyValueFromAddresses(
        Stream<Optional<SWOrder.Address>> addressStream,
        Function<Optional<SWOrder.Address>, String> fetchFn
    ) {
        return addressStream
            .map(fetchFn)
            .filter(s -> !s.isEmpty())
            .findFirst();
    }
    
    private List<MBOrder.Product> mapOrderDetails(List<SWOrder.Article> articles) {
        return articles.stream()
            .filter(SWOrder.Article::hasPositivePrice)
            .map(this::mapOrderDetail)
            .collect(Collectors.toList());
    }
    
    private MBOrder.Product mapOrderDetail(SWOrder.Article article) {
        return new MBOrder.Product(
            article.getArticleNumber(),
            article.getArticleName(),
            article.getPrice(),
            article.getQuantity()
        );   
    }

    private Disposable startCustomerStream(RateLimiter rl) throws IOException {
        Flux<CustomerListItem> customerStream = customerStreamFactory.create();

        return customerStream
            .flatMap(cli -> {
                try {
                    return Mono.just(mapCustomerToLead(cli));
                } catch (Throwable e) {
                    return Mono.empty();
                }
            })
            .transform(RateLimitElements.with(rl))
            .delayElements(Duration.ofMillis(60 * 1000 / mbProps.getRequestsPerMinute()))
            .flatMap(lead -> this.mbAPI.createLead(lead)
                .thenReturn(lead)
                .onErrorResume(e -> Mono.empty()))
            .subscribe(lead -> {
                logger.info("Finished handling lead: {}", lead.getEmail());
            });
    }

    private MBLead mapCustomerToLead(CustomerListItem customer) {
        return new MBLead(
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            true
        );
    }

    @Override
    public void destroy() throws Exception {
        orderStreamDisposable.dispose();
        customerStreamDisposable.dispose();
    }

    public static class RateLimitElements<T> implements Function<Flux<T>, Flux<T>> {
        private final RateLimiter rateLimiter;
        private final Duration checkInterval;

        public RateLimitElements(RateLimiter rateLimiter, Duration checkInterval) {
            this.rateLimiter = rateLimiter;
            this.checkInterval = checkInterval;
        }

        public RateLimitElements(RateLimiter rateLimiter) {
            this(rateLimiter, Duration.ofMillis(10));
        }

        public static <T> RateLimitElements<T> with(RateLimiter rateLimiter) {
            return new RateLimitElements<>(rateLimiter);
        }

        public static <T> RateLimitElements<T> with(RateLimiter rateLimiter, Duration checkInterval) {
            return new RateLimitElements<>(rateLimiter, checkInterval);
        }

        @Override
        public Flux<T> apply(Flux<T> flux) {
            return flux.delayUntil(v -> Mono.create(sink -> {
                Disposable disposable = Flux.interval(checkInterval)
                    .filter(n -> rateLimiter.acquirePermission())
                    .subscribe(n -> sink.success());

                sink.onDispose(disposable);
            }));
        }
    }

    public static class OrderStreamElement {
        private SWOrder swOrder;
        private MBOrder mbOrder;

        public OrderStreamElement(SWOrder swOrder, MBOrder mbOrder) {
            this.swOrder = swOrder;
            this.mbOrder = mbOrder;
        }

        public OrderStreamElement(SWOrder swOrder) {
            this(swOrder, null);
        }

        public SWOrder getSwOrder() {
            return swOrder;
        }

        public OrderStreamElement setSwOrder(SWOrder swOrder) {
            this.swOrder = swOrder;
            return this;
        }

        public MBOrder getMbOrder() {
            return mbOrder;
        }

        public OrderStreamElement setMbOrder(MBOrder mbOrder) {
            this.mbOrder = mbOrder;
            return this;
        }
    }
}
