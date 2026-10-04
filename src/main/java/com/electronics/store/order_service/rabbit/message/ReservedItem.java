package com.electronics.store.order_service.rabbit.message;

import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservedItem(
        @NonNull UUID itemId,
        int quantity,
        @NonNull BigDecimal price,
        @NonNull String description,
        @NonNull String itemUrl) {
}
