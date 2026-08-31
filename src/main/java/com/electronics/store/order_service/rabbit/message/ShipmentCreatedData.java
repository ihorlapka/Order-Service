package com.electronics.store.order_service.rabbit.message;

import java.time.OffsetDateTime;

public record ShipmentCreatedData(
        String trackingNumber,
        String carrier,
        OffsetDateTime estimatedDelivery) implements EventData {
}
