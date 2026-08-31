package com.electronics.store.order_service.rabbit.message;

public record ShipmentFailedData(
        String reason,
        String errorCode) implements EventData {
}
