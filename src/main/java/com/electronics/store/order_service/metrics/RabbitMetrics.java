package com.electronics.store.order_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RabbitMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter rabbitMessagesPublished;
    private final Counter rabbitMessagesFailed;
    private final Counter rabbitMessagesConsumed;
    private final Counter rabbitMessagesRejected;
    private final Timer sagaProcessingTimer;

    public RabbitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.rabbitMessagesPublished = Counter.builder("rabbit.messages.published.total")
                .description("Total number of RabbitMQ messages published")
                .register(meterRegistry);

        this.rabbitMessagesFailed = Counter.builder("rabbit.messages.failed.total")
                .description("Total number of RabbitMQ messages failed to publish")
                .register(meterRegistry);

        this.rabbitMessagesConsumed = Counter.builder("rabbit.messages.consumed.total")
                .description("Total number of RabbitMQ messages consumed")
                .register(meterRegistry);

        this.rabbitMessagesRejected = Counter.builder("rabbit.messages.rejected.total")
                .description("Total number of RabbitMQ messages rejected")
                .register(meterRegistry);

        this.sagaProcessingTimer = Timer.builder("saga.processing.duration")
                .description("Time taken to process saga event")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public void incrementRabbitMessagesPublished() {
        rabbitMessagesPublished.increment();
    }

    public void incrementRabbitMessagesFailed() {
        rabbitMessagesFailed.increment();
    }

    public void incrementRabbitMessagesConsumed() {
        rabbitMessagesConsumed.increment();
    }

    public void incrementRabbitMessagesRejected() {
        rabbitMessagesRejected.increment();
    }

    public Timer.Sample startSagaProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopSagaProcessingTimer(Timer.Sample sample) {
        sample.stop(sagaProcessingTimer);
    }

    public double getRabbitQueueSize() {
        return meterRegistry.get("rabbitmq.queue.size").gauge().value();
    }

    public double getRabbitConnectionCount() {
        return meterRegistry.get("rabbitmq.connections.open").gauge().value();
    }
}