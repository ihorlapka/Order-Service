package com.electronics.store.order_service.controllers.dto;

import lombok.NonNull;

import java.util.UUID;

public record RequestItem(
        @NonNull UUID itemId,
        int quantity
) {
}
