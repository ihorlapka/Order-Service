package com.electronics.store.order_service.rabbit.message;

import java.util.Set;

public record InventoryReservedData(
        Set<ReservedItem> reservedItems) implements EventData {
}
