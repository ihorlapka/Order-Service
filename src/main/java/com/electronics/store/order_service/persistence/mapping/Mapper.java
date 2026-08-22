package com.electronics.store.order_service.persistence.mapping;

import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.dto.OrderItemDto;
import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import lombok.experimental.UtilityClass;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class Mapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static OrderEvent mapToOrderEvent(Order order, OrderEventType eventType) {
        final OrderEvent orderEvent = new OrderEvent();
        orderEvent.setEventType(eventType);
        orderEvent.setOrderId(order.getId());
        final String orderJson = OBJECT_MAPPER.writeValueAsString(mapToOrderDto(order));
        orderEvent.setPayload(orderJson);
        return orderEvent;
    }

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
