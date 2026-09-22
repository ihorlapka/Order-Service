package com.electronics.store.outbox_event_publisher;

import com.electronics.store.outbox_event_publisher.event.Event;
import com.electronics.store.outbox_event_publisher.event.EventService;
import com.electronics.store.outbox_event_publisher.rabbit.RabbitMqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
public class OutboxProcessor<E extends Event> {

    private final RabbitMqPublisher publisher;
    private final EventService<E> eventService;
    private final int eventsBatchSize;


    @Transactional
    public ProcessingResult processBatch() {
        final List<E> freshEvents = eventService.findFreshEventsForUpdate(eventsBatchSize);
        if (freshEvents.isEmpty()) {
            return new ProcessingResult(false, false);
        }
        final List<UUID> publishedIds = new ArrayList<>();
        boolean hasError = false;
        for (Event event : freshEvents) {
            log.info("Sending outbox event msg: {}", event);
            try {
                publisher.publish(event.getOrderId(), event.getPayload());
                publishedIds.add(event.getId());
            } catch (Exception e) {
                log.error("Error sending outbox event msg with eventId: {}", event.getId(), e);
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
