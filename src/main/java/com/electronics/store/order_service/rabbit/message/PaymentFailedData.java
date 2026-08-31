package com.electronics.store.order_service.rabbit.message;

public record PaymentFailedData(
        String reason,
        String errorCode) implements EventData {
}
