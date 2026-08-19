package com.electronics.store.order_service.controllers.dto;

import com.electronics.store.order_service.persistence.enums.Currency;
import com.electronics.store.order_service.persistence.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID customerId,
        OrderStatus status,
        OffsetDateTime createdAt,
        Currency currency,
        BigDecimal totalPrice,
        Set<OrderItemDto> orderItems
) {
}
