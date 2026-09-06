package com.electronics.store.order_service.inventory;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemService {

    public Map<UUID, Item> getItemsByIds(Set<UUID> itemIds) { //todo: implement!
        return Map.of();
    }

    public InventoryResponse reserve(Set<RequestItem> requestItems) {
        return null;
    }

    public InventoryResponse release(Set<RequestItem> requestItems) {
        return null;
    }
}
