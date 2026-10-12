package com.electronics.store.outbox_event_publisher.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    private final MeterRegistry meterRegistry;

    private final Timer outboxProcessingTimer;

    public OutboxMetrics(MeterRegistry meterRegistry, String serviceName) {
        this.meterRegistry = meterRegistry;

        this.outboxProcessingTimer = Timer.builder("outbox_processing_duration")
                .description("Time taken to process outbox events batch")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("service_name", serviceName)
                .register(meterRegistry);
    }

    public Timer.Sample startOutboxProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOutboxProcessingTimer(Timer.Sample sample) {
        sample.stop(outboxProcessingTimer);
    }
}