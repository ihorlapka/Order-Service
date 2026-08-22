package com.electronics.store.order_service.controllers.dto;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        @NonNull UUID itemId,
        @NonNull String description,
        int quantity,
        @NonNull BigDecimal price,
        byte[] image,
        @NonNull String url
) {
}
