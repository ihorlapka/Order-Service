package com.electronics.store.order_service.metrics;

import com.electronics.store.order_service.persistence.enums.EventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CustomRabbitMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter processedInventoryReserved;
    private final Counter processedInventoryFailed;
    private final Counter processedPaymentCompleted;
    private final Counter processedPaymentFailed;
    private final Counter processedShipmentCreated;
    private final Counter processedShipmentFailed;
    private final Counter processedShipmentCompleted;
    private final Counter rejectedMessagesCounter;
    private final Timer messagePublishTimer;
    private final Timer messageDeliveryTimer;


    public CustomRabbitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processedInventoryReserved = Counter.builder("orders_inventory_reserved_processed")
                .description("Total number of processed inventory reserved messages")
                .register(meterRegistry);

        this.processedInventoryFailed = Counter.builder("orders_inventory_failed_processed")
                .description("Total number of processed inventory failed messages")
                .register(meterRegistry);

        this.processedPaymentCompleted = Counter.builder("orders_payment_completed_processed")
                .description("Total number of processed payment completed messages")
                .register(meterRegistry);

        this.processedPaymentFailed = Counter.builder("orders_payment_failed_processed")
                .description("Total number of processed payment failed messages")
                .register(meterRegistry);

        this.processedShipmentCreated = Counter.builder("orders_shipment_created_processed")
                .description("Total number of processed shipment created messages")
                .register(meterRegistry);

        this.processedShipmentFailed = Counter.builder("orders_shipment_failed_processed")
                .description("Total number of processed shipment failed messages")
                .register(meterRegistry);

        this.processedShipmentCompleted = Counter.builder("orders_shipment_completed_processed")
                .description("Total number of processed shipment completed messages")
                .register(meterRegistry);

        this.rejectedMessagesCounter = Counter.builder("inventory_invalid_messages_total")
                .description("Total number of invalid messages sent to DLQ")
                .register(meterRegistry);

        this.messagePublishTimer = Timer.builder("inventory_rabbitmq_message_publish_duration")
                .description("Time taken to publish a message to RabbitMQ")
                .tag("exchange", "es.inventory.events.exchange")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.messageDeliveryTimer = Timer.builder("orders_message_delivery_time")
                .description("Time taken to deliver message from other services to order service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementProcessedMessages(@NonNull EventType eventType) {
        switch (eventType) {
            case INVENTORY_RESERVED -> processedInventoryReserved.increment();
            case INVENTORY_FAILED -> processedInventoryFailed.increment();
            case PAYMENT_COMPLETED -> processedPaymentCompleted.increment();
            case PAYMENT_FAILED -> processedPaymentFailed.increment();
            case SHIPMENT_CREATED -> processedShipmentCreated.increment();
            case SHIPMENT_FAILED -> processedShipmentFailed.increment();
            case SHIPMENT_COMPLETED -> processedShipmentCompleted.increment();
            default -> log.error("Unable to record processed message metric, invalid event type: {}", eventType);
        }
    }

    public void recordMessagePublishDuration(Timer.Sample sample) {
        sample.stop(messagePublishTimer);
    }

    public Timer.Sample startPublishTimer() {
        return Timer.start(meterRegistry);
    }

    public void incrementInvalidMessages() {
        rejectedMessagesCounter.increment();
    }

    public void recordDeliveryTime(long deliveryTime) {
        messageDeliveryTimer.record(deliveryTime, TimeUnit.MILLISECONDS);
    }
}