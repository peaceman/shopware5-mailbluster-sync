package com.n2305.swmb.shopware;

import com.n2305.swmb.properties.AppProperties;
import com.n2305.swmb.properties.ShopwareProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class CustomerPublisher implements Consumer<FluxSink<CustomerListItem>> {
    private static final Logger logger = LoggerFactory.getLogger(CustomerPublisher.class);

    private final ShopwareAPI api;
    private final ShopwareProperties swProps;
    private final AppProperties appProps;
    private final ConcurrentLinkedQueue<CustomerListItem> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean fetchInFlight = new AtomicBoolean(false);
    private final AtomicLong lastFetchedID;

    private FluxSink<CustomerListItem> sink;
    private Disposable checkIntervalDisposable;
    private Disposable stateStoreIntervalDisposable;

    public CustomerPublisher(ShopwareAPI api, ShopwareProperties swProps, AppProperties appProps) throws IOException {
        this.api = api;
        this.swProps = swProps;
        this.appProps = appProps;

        this.lastFetchedID = new AtomicLong(readLastFetchedIDFromFile());
    }

    private synchronized long readLastFetchedIDFromFile() throws IOException {
        File file = getLastFetchedIDFile();

        // todo maybe scanner?
        try (FileInputStream is = new FileInputStream(file)) {
            try {
                return Long.parseLong(new String(is.readAllBytes()).trim());
            } catch (NumberFormatException e) {
                logger.warn("Failed to read last fetched id; start with 0", e);

                return 0L;
            }
        }
    }

    private File getLastFetchedIDFile() throws IOException {
        Path path = Path.of(appProps.getStateFolderPath(), "last-fetched-id.txt");
        File file = path.toFile();

        file.createNewFile();

        return file;
    }

    private synchronized void saveLastFetchedIDToFile() throws IOException {
        File file = getLastFetchedIDFile();

        // todo maybe writer?
        try (FileOutputStream os = new FileOutputStream(file, false)) {
            os.getChannel().truncate(0);
            os.write(String.valueOf(lastFetchedID.get()).getBytes());
        }
    }

    @Override
    public void accept(FluxSink<CustomerListItem> sink) {
        this.sink = sink;

        sink.onRequest(this::onRequest);
        sink.onDispose(this::onSinkDispose);

        setupCheckInterval();
        setupStateStoreInterval();
    }

    private void setupCheckInterval() {
        setupCheckInterval(Duration.ofMillis(1));
    }

    private void setupCheckInterval(Duration checkInterval) {
        dispose(checkIntervalDisposable);

        checkIntervalDisposable = Flux.interval(checkInterval)
            .subscribe(n -> this.updateQueue());
    }

    private void setupStateStoreInterval() {
        dispose(stateStoreIntervalDisposable);

        stateStoreIntervalDisposable = Flux.interval(Duration.ofSeconds(5))
            .subscribe(n -> this.storeState());
    }

    private void dispose(Disposable disposable) {
        Optional.ofNullable(disposable)
            .filter(d -> !d.isDisposed())
            .ifPresent(Disposable::dispose);
    }

    private void storeState() {
        try {
            this.saveLastFetchedIDToFile();
        } catch (IOException e) {
            logger.warn("Failed to save state", e);
        }
    }

    private void updateQueue() {
        logger.debug(
            "Updating queue, queue size {} requested {} inflight {}",
            queue.size(), sink.requestedFromDownstream(), fetchInFlight.get()
        );

        long requestedFromDownstream = sink.requestedFromDownstream();

        if (queue.isEmpty()
            && requestedFromDownstream > 0
            && fetchInFlight.compareAndSet(false, true)
        ) {
            fetch();
        } else {
            fillSinkFromQueue(requestedFromDownstream);
        }
    }

    private void fetch() {
        api.fetchCustomers(buildFilters())
            .doFinally(signalType -> fetchInFlight.set(false))
            .onErrorResume(e -> Mono.empty())
            .subscribe(this::handleListItems);
    }

    private List<ShopwareAPI.Filter> buildFilters() {
        List<ShopwareAPI.Filter> filters = new LinkedList<>();
        filters.add(new ShopwareAPI.Filter(
            "id",
            String.valueOf(lastFetchedID.get()),
            ">"
        ));

        filters.addAll(swProps.getCustomerFilters());

        return filters;
    }

    private void handleListItems(List<CustomerListItem> list) {
        adjustCheckInterval(list);

        list.forEach(i -> {
            queue.add(i);

            onQueueAdd(i);
        });
    }

    private void adjustCheckInterval(List<?> list) {
        if (list.isEmpty()) {
            setupCheckInterval(swProps.getIntervals().getOnEmptyList());
        } else {
            setupCheckInterval();
        }
    }

    private void onQueueAdd(CustomerListItem item) {
        lastFetchedID.updateAndGet(prev -> Math.max(prev, item.getId()));
    }

    private void fillSinkFromQueue(long n) {
        int i;
        for (i = 0; i < n; i++) {
            if (queue.isEmpty())
                break;

            CustomerListItem item = queue.poll();
            if (item == null) continue;

            logger.info("Publish into sink: {}", item);
            sink.next(item);
        }

        if (i > 0) {
            logger.info("Published {} items into sink", i);
        }
    }

    private void onRequest(long n) {
        logger.debug("Got request for {} items, queue size {}", n, queue.size());

        updateQueue();
    }

    private void onSinkDispose() {
        dispose(checkIntervalDisposable);
        dispose(stateStoreIntervalDisposable);
    }
}
