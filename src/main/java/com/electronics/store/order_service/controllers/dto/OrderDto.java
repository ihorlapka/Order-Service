package com.electronics.store.order_service.controllers.dto;

import com.electronics.store.order_service.persistence.enums.Currency;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record OrderDto(
        @NonNull UUID id,
        @NonNull UUID customerId,
        @NonNull OrderStatus status,
        @NonNull OffsetDateTime createdAt,
        @NonNull Currency currency,
        @NonNull BigDecimal totalPrice,
        @NonNull Set<OrderItemDto> orderItems
) {
}
