package com.electronics.store.order_service.persistence.mapping;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.enums.PublishmentStatus;
import com.electronics.store.order_service.persistence.enums.OutboxEventType;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.electronics.store.order_service.persistence.enums.OrderStatus.PENDING;
import static com.electronics.store.order_service.persistence.enums.OutboxEventType.ORDER_CANCELLED;
import static com.electronics.store.order_service.persistence.enums.OutboxEventType.ORDER_CREATED;
import static com.electronics.store.order_service.persistence.enums.PublishmentStatus.NEW;
import static com.electronics.store.order_service.rabbit.message.EventDataResolver.resolveEventData;
import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptySet;

@Slf4j
@UtilityClass
public class EntityCreator {

    public static Order createOrder(CreateOrderRequest request) {
        final Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(PENDING);
        order.setCreatedAt(now());
        order.setCurrency(request.currency());
        final Set<OrderItem> orderItems = createOrderItems(request.requestItems(), order);
        order.setItems(orderItems);
        return order;
    }

    public static Set<OrderItem> createOrderItems(Set<RequestItem> requestItems, Order order) {
        final Set<OrderItem> orderItems = new HashSet<>(requestItems.size());
        for (RequestItem requestItem : requestItems) {
            orderItems.add(new OrderItem(null, requestItem.itemId(), requestItem.quantity(), null, null, order));
        }
        return orderItems;
    }

    public static OutboxEvent createOutboxEventForNewOrder(Order order) {
        return createOutboxEvent(order, emptySet(), ORDER_CREATED, NEW);
    }

    public static OutboxEvent createOutboxEventForCancelledOrder(Order order) {
        return createOutboxEvent(order, emptySet(), ORDER_CANCELLED, NEW);
    }

    public static OutboxEvent createOutboxEvent(Order order, Set<OrderItem> itemsToUpdate,
                                                OutboxEventType eventType, PublishmentStatus status) {
        final OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setEventType(eventType);
        outboxEvent.setOrderId(order.getId());
        outboxEvent.setCreatedAt(now());
        final String orderJson = createPayload(order, itemsToUpdate, outboxEvent);
        outboxEvent.setPayload(orderJson);
        outboxEvent.setStatus(status);
        return outboxEvent;
    }

    private static String createPayload(Order order, Set<OrderItem> itemsToUpdate, OutboxEvent outboxEvent) {
        final OrderStatus resolvedOrderStatus = OrderStatusResolver.resolve(outboxEvent.getEventType(), order.getStatus());
        final MessageEvent messageEvent = new MessageEvent(outboxEvent.getId(), outboxEvent.getEventType(), order.getId(),
                resolvedOrderStatus, outboxEvent.getCreatedAt(), resolveEventData(order, itemsToUpdate, outboxEvent));
        return PayloadPatcher.serialize(messageEvent);
    }
}
