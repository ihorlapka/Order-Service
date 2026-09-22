package com.electronics.store.outbox_event_publisher;

import lombok.NonNull;

import java.util.UUID;

public record PublishmentTriggerEvent(@NonNull UUID orderId) {
}
