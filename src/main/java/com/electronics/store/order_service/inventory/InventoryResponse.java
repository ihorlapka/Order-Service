package com.electronics.store.order_service.inventory;

import lombok.NonNull;

import java.util.Map;
import java.util.UUID;

public record InventoryResponse(
        boolean success,
        String message,
        Map<UUID, Item> itemsByIds) {
}
