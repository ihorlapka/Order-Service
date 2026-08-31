package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageEvent(
        UUID eventId,
        OrderEventType eventType,
        UUID orderId,
        OrderStatus orderStatus,
        OffsetDateTime createdAt,
        EventData eventData) {

}
