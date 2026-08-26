package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.events.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OutboxEventManager outboxEventManager;

    @TransactionalEventListener
    public void handleEvents(OrderCreatedEvent event) {
        log.info("Received application OrderCreatedEvent: {}", event);
        outboxEventManager.publishAndUpdate();
    }
}
