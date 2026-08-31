package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.Currency;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record OrderCreatedData(
        UUID customerId,
        Currency currency,
        BigDecimal totalPrice,
        Set<EventItem> items) implements EventData {
}
