package com.electronics.store.order_service.grpc;


import java.math.BigDecimal;
import java.util.UUID;

public record Item(
        UUID id,
        String description,
        int totalAmount,
        BigDecimal price,
        byte[] imageData,
        String itemUrl
) {
}
