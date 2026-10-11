package com.electronics.store.outbox_event_publisher.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter outboxEventsPublished;
    private final Counter outboxEventsFailed;
    private final Timer outboxProcessingTimer;

    public OutboxMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.outboxEventsPublished = Counter.builder("outbox.events.published.total")
                .description("Total number of outbox events published")
                .register(meterRegistry);

        this.outboxEventsFailed = Counter.builder("outbox.events.failed.total")
                .description("Total number of outbox events failed to publish")
                .register(meterRegistry);

        this.outboxProcessingTimer = Timer.builder("outbox.processing.duration")
                .description("Time taken to process outbox events batch")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public void incrementOutboxEventsPublished() {
        outboxEventsPublished.increment();
    }

    public void incrementOutboxEventsFailed() {
        outboxEventsFailed.increment();
    }

    public Timer.Sample startOutboxProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOutboxProcessingTimer(Timer.Sample sample) {
        sample.stop(outboxProcessingTimer);
    }
}