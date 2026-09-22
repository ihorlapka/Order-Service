package com.electronics.store.outbox_event_publisher.event;

import java.util.UUID;

public interface Event {

    UUID id();
    String payload();
    UUID orderId();
}
