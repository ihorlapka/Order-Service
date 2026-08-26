package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.service.OrderEventService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventManager {

    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "outbox-publisher"));

    private final OutboxProcessor outboxProcessor;


    public void publishAndUpdate() {
        if (!processing.compareAndSet(false, true)) {
            log.debug("Outbox processing already in progress, skipping trigger event");
            return;
        }
        executorService.submit(() -> {
                    try {
                        boolean hasMore = true;
                        while (hasMore) {
                            hasMore = outboxProcessor.processBatch();
                        }
                        log.info("OutboxProcessor stopped");
                    } catch (Exception e) {
                        log.error("Error processing order outbox events", e);
                    } finally {
                        processing.set(false);
                    }
                }
        );
        log.info("Started outbox order events processing...");
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        executorService.shutdown();
        boolean isShutdown = executorService.awaitTermination(5, TimeUnit.SECONDS);
        if (isShutdown) {
            log.info("Outbox executor service has been shut down gracefully");
        } else {
            log.info("Forcing shutdown of outbox executor service after time has elapsed");
            executorService.shutdownNow();
        }
    }
}
