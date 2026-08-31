package com.electronics.store.order_service.controllers.dto;

import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderItem;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class DtoMapper {

    public static OrderDto mapToOrderDto(Order order) {
        final Set<OrderItemDto> itemDtos = mapToOrderItemDtos(order);
        return new OrderDto(order.getId(), order.getCustomerId(), order.getStatus(), order.getCreatedAt(),
                order.getCurrency(), order.getTotalPrice(), itemDtos);
    }

    private static Set<OrderItemDto> mapToOrderItemDtos(Order order) {
        final Set<OrderItemDto> itemDtos = new HashSet<>(order.getItems().size());
        for (OrderItem item : order.getItems()) {
            itemDtos.add(new OrderItemDto(null, item.getItemId(), item.getDescription(),
                    item.getQuantity(), item.getPrice(), item.getImageData(), item.getItemUrl()));
        }
        return itemDtos;
    }
}
