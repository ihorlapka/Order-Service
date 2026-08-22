package com.electronics.store.order_service.grpc;


import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

public record Item(
        @NonNull UUID id,
        @NonNull String description,
        int totalAmount,
        @NonNull BigDecimal price,
        byte[] imageData,
        @NonNull String itemUrl
) {
}
