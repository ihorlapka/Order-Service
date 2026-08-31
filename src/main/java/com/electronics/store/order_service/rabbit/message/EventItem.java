package com.electronics.store.order_service.rabbit.message;

import java.math.BigDecimal;
import java.util.UUID;

public record EventItem(
        UUID id,
        UUID itemId,
        String description,
        int quantity,
        BigDecimal price,
        String itemUrl) {
}
