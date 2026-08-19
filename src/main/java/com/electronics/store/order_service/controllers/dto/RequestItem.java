package com.electronics.store.order_service.controllers.dto;

import java.util.UUID;

public record RequestItem(
        UUID itemId,
        int quantity
) {
}
