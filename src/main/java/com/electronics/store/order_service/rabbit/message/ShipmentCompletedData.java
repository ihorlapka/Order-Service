package com.electronics.store.order_service.rabbit.message;

import java.time.OffsetDateTime;

public record ShipmentCompletedData(
        OffsetDateTime deliveredAt,
        String signedBy) implements EventData {
}
