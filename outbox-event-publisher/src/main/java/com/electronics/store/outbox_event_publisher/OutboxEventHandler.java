package com.electronics.store.outbox_event_publisher;

import com.electronics.store.outbox_event_publisher.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventHandler<E extends Event> {

    private final OutboxEventManager<E> outboxEventManager;

    @TransactionalEventListener
    public void handleCreatedEvents(PublishmentTriggerEvent event) {
        log.info("Received application OrderCreatedEvent: {}", event);
        outboxEventManager.publishAndUpdate();
    }
}
