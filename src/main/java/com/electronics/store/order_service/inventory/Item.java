package com.electronics.store.order_service.inventory;


import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

public record Item(
        @NonNull UUID id,
        @NonNull String description,
        boolean isReserved,
        int availableAmount,
        @NonNull BigDecimal price,
        byte[] imageData,
        @NonNull String itemUrl
) {
}
