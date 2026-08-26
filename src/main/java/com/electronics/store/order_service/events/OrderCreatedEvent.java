package com.electronics.store.order_service.events;

import lombok.NonNull;

import java.util.UUID;

public record OrderCreatedEvent(@NonNull UUID orderId) {
}
