package com.electronics.store.order_service.controllers.misc;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.UUID;

@Validated
public record UpdateOrderRequest(
        @NonNull UUID requestId,
        @NonNull UUID orderId,
        Set<RequestItem> orderItemsToBeAdded,
        Set<RequestItem> orderItemsToBeRemoved) {
}
