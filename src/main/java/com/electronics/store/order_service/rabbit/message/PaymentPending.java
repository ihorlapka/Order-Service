package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.Currency;

import java.math.BigDecimal;

public record PaymentPending(
        Currency currency,
        BigDecimal amount) implements EventData {
}
