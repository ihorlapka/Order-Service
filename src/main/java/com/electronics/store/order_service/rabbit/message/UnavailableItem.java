package com.electronics.store.order_service.rabbit.message;

import java.util.UUID;

public record UnavailableItem(
        UUID itemId,
        int requestedQuantity,
        int availableQuantity) {
}
