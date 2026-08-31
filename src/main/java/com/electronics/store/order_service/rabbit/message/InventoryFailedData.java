package com.electronics.store.order_service.rabbit.message;

import java.util.Set;

public record InventoryFailedData(
        Set<UnavailableItem> unavailableItems,
        String reason) implements EventData {
}
