package com.electronics.store.order_service.rabbit.message;

import java.util.UUID;

public record EventItem(
        UUID id,
        UUID itemId,
        int quantity) {
}
