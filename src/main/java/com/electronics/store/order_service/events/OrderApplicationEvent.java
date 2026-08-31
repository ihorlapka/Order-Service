package com.electronics.store.order_service.events;

public sealed interface OrderApplicationEvent permits OrderCreatedEvent, OrderCancelledEvent {
}
