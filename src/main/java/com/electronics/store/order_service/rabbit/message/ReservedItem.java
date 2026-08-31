package com.electronics.store.order_service.rabbit.message;

import java.util.UUID;

public record ReservedItem(
        UUID itemId,
        int quantity) {
}
