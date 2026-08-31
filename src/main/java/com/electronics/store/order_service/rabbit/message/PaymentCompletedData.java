package com.electronics.store.order_service.rabbit.message;

import com.electronics.store.order_service.persistence.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletedData(
        UUID transactionId,
        Currency currency,
        BigDecimal amount,
        String paymentMethod) implements EventData {
}
