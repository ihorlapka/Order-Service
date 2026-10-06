package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.Currency;
import lombok.NonNull;

import java.util.Set;
import java.util.UUID;

public record OrderCreatedData(
        @NonNull UUID customerId,
        @NonNull Currency currency,
        @NonNull Set<EventItem> items) implements EventData {
}
