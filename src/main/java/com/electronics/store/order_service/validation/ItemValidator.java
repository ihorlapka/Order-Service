package com.electronics.store.order_service.validation;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.Item;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemValidator {


    public boolean isValid(CreateOrderRequest request, Map<UUID, Item> itemsById) {
        if (request.orderItems().size() != itemsById.size()) {
            log.warn("Not all items from requestId: {} are present in inventory, missed itemsIds: {}",
                    request.requestId(), getMissedItems(request, itemsById));
            return false;
        }
        for (RequestItem requestItem : request.orderItems()) {
            final Item actualItem = itemsById.get(requestItem.itemId());
            if (actualItem == null) {
                log.warn("Item with id: {} was not found", requestItem.itemId());
                return false;
            }
            if (actualItem.totalAmount() < requestItem.quantity()) {
                log.warn("Not enough amount of items in inventory itemId: {}", requestItem.itemId());
                return false;
            }
        }
        return true;
    }

    private boolean getMissedItems(CreateOrderRequest request, Map<UUID, Item> itemsById) {
        return Sets.newHashSet(request.orderItems().stream()
                .map(RequestItem::itemId)
                .collect(toSet()))
                .removeAll(itemsById.keySet());
    }
}
