package com.electronics.store.order_service.rabbit.message;

import lombok.NonNull;

public record OrderCancelledData(
        @NonNull String reason) implements EventData {
}
