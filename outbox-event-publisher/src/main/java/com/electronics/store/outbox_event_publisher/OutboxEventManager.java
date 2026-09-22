package com.electronics.store.outbox_event_publisher;

import com.electronics.store.outbox_event_publisher.event.Event;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventManager<E extends Event> {

    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "outbox-processor"));

    private final OutboxProcessor<E> outboxProcessor;
    private final int timeoutAfterFailure;


    public void publishAndUpdate() {
        if (!processing.compareAndSet(false, true)) {
            log.debug("Outbox processing already in progress, skipping trigger event");
            return;
        }
        executorService.submit(() -> {
                    try {
                        boolean hasMore = true;
                        while (hasMore) {
                            final OutboxProcessor.ProcessingResult result = outboxProcessor.processBatch();
                            hasMore = result.hasMore();
                            if (result.hasError()) {
                                Thread.sleep(timeoutAfterFailure);
                            }
                        }
                        log.info("OutboxProcessor stopped");
                    } catch (Exception e) {
                        log.error("Error processing order outbox events", e);
                    } finally {
                        processing.set(false);
                    }
                }
        );
        log.info("Started outbox events messages processing...");
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        executorService.shutdown();
        boolean isShutdown = executorService.awaitTermination(5, SECONDS);
        if (isShutdown) {
            log.info("Outbox executor service has been shut down gracefully");
        } else {
            log.info("Forcing shutdown of outbox executor service after time has elapsed");
            executorService.shutdownNow();
        }
    }
}
