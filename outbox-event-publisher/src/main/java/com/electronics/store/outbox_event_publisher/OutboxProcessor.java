package com.electronics.store.outbox_event_publisher;

import com.electronics.store.outbox_event_publisher.event.Event;
import com.electronics.store.outbox_event_publisher.event.EventService;
import com.electronics.store.outbox_event_publisher.rabbit.RabbitMqPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
public class OutboxProcessor {

    private final RabbitMqPublisher publisher;
    private final EventService<Event> eventService;
    private final int eventsBatchSize;

    public OutboxProcessor(RabbitMqPublisher publisher, EventService<Event> eventService,
                           @Value("${outbox.events.batch.size}") int eventsBatchSize) {
        this.publisher = publisher;
        this.eventService = eventService;
        this.eventsBatchSize = eventsBatchSize;
    }


    @Transactional
    public ProcessingResult processBatch() {
        final List<Event> freshEvents = eventService.findFreshEventsForUpdate(eventsBatchSize);
        if (freshEvents.isEmpty()) {
            return new ProcessingResult(false, false);
        }
        final List<UUID> publishedIds = new ArrayList<>();
        boolean hasError = false;
        for (Event event : freshEvents) {
            log.info("Sending outbox event msg: {}", event);
            try {
                publisher.publish(event.orderId(), event.payload());
                publishedIds.add(event.id());
            } catch (Exception e) {
                log.error("Error sending outbox event msg with eventId: {}", event.id(), e);
                hasError = true;
            }
        }
        log.info("Finished processing outbox events with orderIds: {}", publishedIds);
        int updatedRows = eventService.updatePublishedEvents(publishedIds);
        if (updatedRows != publishedIds.size()) {
            log.warn("Expected to be updated: {} outbox events but were {}, missed events will " +
                    "be retried in next iteration", publishedIds, updatedRows);
        }
        log.info("Updated event status to PUBLISHED for events with eventIds: {}", publishedIds);
        return new ProcessingResult(true, hasError);
    }

    public record ProcessingResult(boolean hasMore, boolean hasError) {}
}
