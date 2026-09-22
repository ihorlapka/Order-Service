package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.OutboxEventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import lombok.NonNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageEvent(
        @NonNull UUID eventId,
        @NonNull OutboxEventType eventType,
        @NonNull UUID orderId,
        @NonNull OrderStatus orderStatus,
        @NonNull OffsetDateTime createdAt,
        @NonNull EventData eventData) {

}
