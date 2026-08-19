package com.electronics.store.order_service.grpc;

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
}
