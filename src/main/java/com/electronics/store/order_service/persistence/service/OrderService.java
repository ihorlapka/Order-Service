package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.electronics.store.order_service.persistence.enums.OrderEventType.ORDER_CREATED;
import static com.electronics.store.order_service.persistence.enums.OrderStatus.PENDING;
import static com.electronics.store.order_service.persistence.mapping.Mapper.mapToOrderEvent;
import static java.time.OffsetDateTime.now;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventService orderEventService;

    public Optional<Order> findByOrderId(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    @Transactional
    public Order persist(@Valid CreateOrderRequest request, Map<UUID, Item> itemsByIds) {
        final Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(PENDING);
        order.setCreatedAt(now());
        order.setCurrency(request.currency());
        order.setTotalPrice(getTotalPrice(itemsByIds));

        final Set<OrderItem> orderItems = new HashSet<>(request.orderItems().size());
        for (RequestItem requestItem : request.orderItems()) {
            final Item actualItem = itemsByIds.get(requestItem.itemId());
            orderItems.add(new OrderItem(null, requestItem.itemId(), actualItem.description(),
                    requestItem.quantity(), actualItem.price(), actualItem.imageData(),
                    actualItem.itemUrl(), order));
        }
        order.setItems(orderItems);

        final Order savedOrder = orderRepository.save(order);
        orderEventService.persist(mapToOrderEvent(savedOrder, ORDER_CREATED));
        return savedOrder;
    }

    public int deleteByOrderId(UUID orderId) {
        return orderRepository.removeById(orderId);
    }

    public List<Order> findByCustomerId(UUID customerId) {
        return orderRepository.findOrdersByCustomerId(customerId);
    }

    private BigDecimal getTotalPrice(Map<UUID, Item> itemsByIds) {
        return itemsByIds.values().stream()
                .map(Item::price)
                .reduce(BigDecimal::add)
                .orElseThrow();
    }
}
