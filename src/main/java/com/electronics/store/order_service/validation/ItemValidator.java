package com.electronics.store.order_service.validation;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.grpc.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemValidator {


    public boolean isValid(Set<RequestItem> requestItems, Map<UUID, Item> itemsById) {
        if (requestItems.size() != itemsById.size()) {
            return false;
        }
        for (RequestItem requestItem : requestItems) {
            final Item actualItem = itemsById.get(requestItem.itemId());
            if (actualItem == null) {
                log.warn("Item with id {} was not found", requestItem.itemId());
                return false;
            }
            if (actualItem.totalAmount() < requestItem.quantity()) {
                log.warn("Not enough amount of items in inventory itemId={}", requestItem.itemId());
                return false;
            }
        }
        return true;
    }
}
