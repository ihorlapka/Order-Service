package com.electronics.store.order_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderServiceMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter ordersCreated;
    private final Counter ordersCancelled;
    private final Counter ordersModified;
    private final Counter ordersReserved;
    private final Counter ordersReservationFailed;
    private final Counter ordersShipped;
    private final Counter ordersDelivered;
    private final Counter ordersDeliveryFailed;
    private final Counter ordersPaid;
    private final Counter ordersPaymentStuck;
    private final Counter outboxEventsPublished;
    private final Counter outboxEventsFailed;
    private final Counter rabbitMessagesPublished;
    private final Counter rabbitMessagesFailed;
    private final Counter rabbitMessagesConsumed;
    private final Counter rabbitMessagesRejected;
    private final Timer orderCreationTimer;
    private final Timer orderCancellationTimer;
    private final Timer orderModificationTimer;
    private final Timer outboxProcessingTimer;
    private final Timer sagaProcessingTimer;

    public OrderServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.ordersCreated = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .register(meterRegistry);

        this.ordersCancelled = Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .register(meterRegistry);

        this.ordersModified = Counter.builder("orders.modified.total")
                .description("Total number of orders modified")
                .register(meterRegistry);

        this.ordersReserved = Counter.builder("orders.reserved.total")
                .description("Total number of orders reserved")
                .register(meterRegistry);

        this.ordersReservationFailed = Counter.builder("orders.reservation_failed.total")
                .description("Total number of orders with reservation failed")
                .register(meterRegistry);

        this.ordersShipped = Counter.builder("orders.shipped.total")
                .description("Total number of orders shipped")
                .register(meterRegistry);

        this.ordersDelivered = Counter.builder("orders.delivered.total")
                .description("Total number of orders delivered")
                .register(meterRegistry);

        this.ordersDeliveryFailed = Counter.builder("orders.delivery_failed.total")
                .description("Total number of orders with delivery failed")
                .register(meterRegistry);

        this.ordersPaid = Counter.builder("orders.paid.total")
                .description("Total number of orders paid")
                .register(meterRegistry);

        this.ordersPaymentStuck = Counter.builder("orders.payment_stuck.total")
                .description("Total number of orders with payment stuck")
                .register(meterRegistry);

        this.outboxEventsPublished = Counter.builder("outbox.events.published.total")
                .description("Total number of outbox events published")
                .register(meterRegistry);

        this.outboxEventsFailed = Counter.builder("outbox.events.failed.total")
                .description("Total number of outbox events failed to publish")
                .register(meterRegistry);

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

        this.orderCreationTimer = Timer.builder("orders.creation.duration")
                .description("Time taken to create an order")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        this.orderCancellationTimer = Timer.builder("orders.cancellation.duration")
                .description("Time taken to cancel an order")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        this.orderModificationTimer = Timer.builder("orders.modification.duration")
                .description("Time taken to modify an order")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        this.outboxProcessingTimer = Timer.builder("outbox.processing.duration")
                .description("Time taken to process outbox events batch")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        this.sagaProcessingTimer = Timer.builder("saga.processing.duration")
                .description("Time taken to process saga event")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
    }

    public void incrementOrdersCreated() {
        ordersCreated.increment();
    }

    public void incrementOrdersCancelled() {
        ordersCancelled.increment();
    }

    public void incrementOrdersModified() {
        ordersModified.increment();
    }

    public void incrementOrdersReserved() {
        ordersReserved.increment();
    }

    public void incrementOrdersReservationFailed() {
        ordersReservationFailed.increment();
    }

    public void incrementOrdersShipped() {
        ordersShipped.increment();
    }

    public void incrementOrdersDelivered() {
        ordersDelivered.increment();
    }

    public void incrementOrdersDeliveryFailed() {
        ordersDeliveryFailed.increment();
    }

    public void incrementOrdersPaid() {
        ordersPaid.increment();
    }

    public void incrementOrdersPaymentStuck() {
        ordersPaymentStuck.increment();
    }

    public void incrementOutboxEventsPublished() {
        outboxEventsPublished.increment();
    }

    public void incrementOutboxEventsFailed() {
        outboxEventsFailed.increment();
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

    public Timer.Sample startOrderCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderCreationTimer(Timer.Sample sample) {
        sample.stop(orderCreationTimer);
    }

    public Timer.Sample startOrderCancellationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderCancellationTimer(Timer.Sample sample) {
        sample.stop(orderCancellationTimer);
    }

    public Timer.Sample startOrderModificationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderModificationTimer(Timer.Sample sample) {
        sample.stop(orderModificationTimer);
    }

    public Timer.Sample startOutboxProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOutboxProcessingTimer(Timer.Sample sample) {
        sample.stop(outboxProcessingTimer);
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

    public double getHibernateSessionCount() {
        return meterRegistry.get("hibernate.sessions.open").gauge().value();
    }

    public double getJdbcConnectionsActive() {
        return meterRegistry.get("hikaricp.connections.active").gauge().value();
    }

    public double getJdbcConnectionsIdle() {
        return meterRegistry.get("hikaricp.connections.idle").gauge().value();
    }
}