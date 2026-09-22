package com.electronics.store.outbox_event_publisher;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class OutboxEventManager {

    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "outbox-processor"));

    private final OutboxProcessor outboxProcessor;
    private final int timeoutAfterFailure;

    public OutboxEventManager(OutboxProcessor outboxProcessor,
                              @Value("${outbox.timeout.after.failure.ms:5000}") Integer timeoutAfterFailureMs) {
        this.outboxProcessor = outboxProcessor;
        this.timeoutAfterFailure = timeoutAfterFailureMs;
    }


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
