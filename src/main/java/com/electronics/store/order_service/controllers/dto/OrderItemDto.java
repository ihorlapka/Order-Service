package com.electronics.store.order_service.controllers.dto;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        @NonNull UUID itemId,
        int quantity,
        @NonNull BigDecimal price,
        @NonNull String url
) {
}
