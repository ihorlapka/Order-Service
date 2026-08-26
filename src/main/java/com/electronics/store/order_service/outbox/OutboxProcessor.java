package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.service.OrderEventService;
import com.electronics.store.order_service.rabbit.RabbitMqPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final RabbitMqPublisher publisher;
    private final OrderEventService eventService;


    @Transactional
    public boolean processBatch() {
        final List<OrderEvent> freshEvents = eventService.findFreshEvents(100);
        if (freshEvents.isEmpty()) {
            return false;
        }
        final List<UUID> publishedIds = new ArrayList<>();
        for (OrderEvent event : freshEvents) {
            log.info("Processing order event: {}", event);
            try {
                publisher.publish(event.getOrderId(), event.getPayload());
                publishedIds.add(event.getOrderId());
            } catch (Exception e) {
                log.error("Error processing order event with orderId: {}", event.getOrderId(), e);
            }
        }
        log.info("Finished processing order events with orderIds: {}", publishedIds);
        eventService.updatePublishedEvents(publishedIds);
        log.info("Updated event status to PUBLISHED for events with orderIds: {}", publishedIds);
        return true;
    }
}
