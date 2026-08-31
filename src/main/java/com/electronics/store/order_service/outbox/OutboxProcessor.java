package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.service.OrderEventService;
import com.electronics.store.order_service.rabbit.RabbitMqPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Component
public class OutboxProcessor {

    private final RabbitMqPublisher publisher;
    private final OrderEventService eventService;
    private final int eventsBatchSize;

    public OutboxProcessor(RabbitMqPublisher publisher, OrderEventService eventService,
                           @Value("${outbox.events.batch.size}") int eventsBatchSize) {
        this.publisher = publisher;
        this.eventService = eventService;
        this.eventsBatchSize = eventsBatchSize;
    }


    @Transactional
    public ProcessingResult processBatch() {
        final List<OrderEvent> freshEvents = eventService.findFreshEvents(eventsBatchSize);
        if (freshEvents.isEmpty()) {
            return new ProcessingResult(false, false);
        }
        final List<UUID> publishedIds = new ArrayList<>();
        boolean hasError = false;
        for (OrderEvent event : freshEvents) {
            log.info("Processing order event: {}", event);
            try {
                publisher.publish(event.getOrderId(), event.getPayload());
                publishedIds.add(event.getId());
            } catch (Exception e) {
                log.error("Error processing order event with eventId: {}", event.getId(), e);
                hasError = true;
            }
        }
        log.info("Finished processing order events with orderIds: {}", publishedIds);
        int updatedRows = eventService.updatePublishedEvents(publishedIds);
        if (updatedRows != publishedIds.size()) {
            log.warn("Expected to be updated: {} order events but were {}, missed events will " +
                    "be retried in next iteration", publishedIds, updatedRows);
        }
        log.info("Updated event status to PUBLISHED for events with orderIds: {}", publishedIds);
        return new ProcessingResult(true, hasError);
    }

    public record ProcessingResult(boolean hasMore, boolean hasError) {}
}
