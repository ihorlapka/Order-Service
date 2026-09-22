package com.electronics.store.outbox_event_publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventHandler {

    private final OutboxEventManager outboxEventManager;

    @TransactionalEventListener
    public void handleCreatedEvents(PublishmentTriggerEvent event) {
        log.info("Received application OrderCreatedEvent: {}", event);
        outboxEventManager.publishAndUpdate();
    }
}
