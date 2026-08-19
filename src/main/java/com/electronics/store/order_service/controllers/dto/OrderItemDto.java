package com.electronics.store.order_service.controllers.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID itemId,
        String description,
        int quantity,
        BigDecimal price,
        byte[] image,
        String url
) {
}
