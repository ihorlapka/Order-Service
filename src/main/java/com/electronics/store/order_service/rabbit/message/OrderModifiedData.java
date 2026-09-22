package com.electronics.store.order_service.rabbit.message;

import java.util.Set;

public record OrderModifiedData(
        Set<EventItem> itemsToUpdate) implements EventData {
}
