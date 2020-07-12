package com.n2305.swmb.shopware;

import com.n2305.swmb.properties.ShopwareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OrderPublisher implements Consumer<FluxSink<OrderListItem>> {
    private static final Logger logger = LoggerFactory.getLogger(OrderPublisher.class);

    private final ShopwareAPI api;
    private final ShopwareProperties swProps;
    private FluxSink<OrderListItem> sink;
    private ConcurrentLinkedQueue<OrderListItem> oliQueue = new ConcurrentLinkedQueue<>();
    private AtomicBoolean fetchOrdersInFlight = new AtomicBoolean(false);
    private OffsetDateTime lastFetchedOrderTime;

    private Disposable checkIntervalDisposable;

    public OrderPublisher(ShopwareAPI api, ShopwareProperties swProps) {
        this.api = api;
        this.swProps = swProps;
    }

    @Override
    public void accept(FluxSink<OrderListItem> sink) {
        this.sink = sink;

        sink.onRequest(this::onRequest);
        sink.onDispose(() -> {
            if (checkIntervalDisposable != null)
                checkIntervalDisposable.dispose();
        });

        setupCheckInterval(Duration.ofMillis(1));
    }

    private void setupCheckInterval(Duration checkInterval) {
        Optional.ofNullable(checkIntervalDisposable)
            .filter(checkIntervalDisposable -> !checkIntervalDisposable.isDisposed())
            .ifPresent(Disposable::dispose);

        checkIntervalDisposable = Flux.interval(checkInterval)
            .subscribe(n -> this.updateQueue());
    }

    private void setupCheckInterval() {
        setupCheckInterval(Duration.ofMillis(1));
    }

    private void onRequest(long n) {
        logger.debug("Got request for {} items, queue size {}", n, oliQueue.size());

        updateQueue();
    }

    private void fillSinkFromQueue(long n) {
        int i;
        for (i = 0; i < n; i++) {
            if (oliQueue.isEmpty())
                break;

            OrderListItem oli = oliQueue.poll();
            if (oli != null) sink.next(oli);
        }

        if (i > 0) {
            logger.info("Published {} items into sink", i);
        }
    }

    private void updateQueue() {
        logger.debug(
            "Updating order queue, queue size {} requested {} inflight {}",
            oliQueue.size(), sink.requestedFromDownstream(), fetchOrdersInFlight.get()
        );

        if (oliQueue.isEmpty()
            && sink.requestedFromDownstream() > 0
            && fetchOrdersInFlight.compareAndSet(false, true)
        ) {
            api.fetchOrders(buildFilters())
                .doFinally(signalType -> fetchOrdersInFlight.set(false))
                .onErrorResume(e -> Mono.empty())
                .subscribe(this::handleOrderListItems);
        } else {
            fillSinkFromQueue(sink.requestedFromDownstream());
        }
    }

    private List<ShopwareAPI.Filter> buildFilters() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        List<ShopwareAPI.Filter> filters = new LinkedList<>();
        filters.add(new ShopwareAPI.Filter("attribute." + swProps.getExportedAttribute(), null));

        if (lastFetchedOrderTime != null) {
            filters.add(new ShopwareAPI.Filter(
                "orderTime", formatter.format(lastFetchedOrderTime), ">"
            ));
        }

        return filters;
    }

    private void handleOrderListItems(List<OrderListItem> orderListItems) {
        if (orderListItems.isEmpty()) {
            setupCheckInterval(Duration.ofSeconds(30));
        } else {
            setupCheckInterval();
        }

        orderListItems.forEach(oli -> {
            oliQueue.add(oli);

            updateLastFetchOrderTime(oli);
        });
    }

    private synchronized void updateLastFetchOrderTime(OrderListItem oli) {
        if (this.lastFetchedOrderTime == null) {
            this.lastFetchedOrderTime = oli.getOrderTime();
        } else if (oli.getOrderTime().isAfter(lastFetchedOrderTime)) {
            this.lastFetchedOrderTime = oli.getOrderTime();
        }
    }
}