package com.electronics.store.outbox_event_publisher;

import java.util.UUID;

public interface EventPublisher {

    void publish(UUID orderId, String payload);
}
