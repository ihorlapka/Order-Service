package com.electronics.store.order_service.controllers;

import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.ItemService;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.mapping.Mapper;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.validation.ItemValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.electronics.store.order_service.persistence.mapping.Mapper.mapToOrderDto;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ItemService itemService;
    private final ItemValidator itemValidator;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to create order {}", request);
        final Map<UUID, Item> itemsByIds = itemService.getItemsByIds(request.orderItems().stream()
                .map(RequestItem::itemId).collect(toSet()));

        if (!itemValidator.isValid(request, itemsByIds)) {
            return ResponseEntity.status(NOT_FOUND).build();
        }
        final Order order = orderService.persist(request, itemsByIds);
        log.info("Created order {}", order);
        return ResponseEntity.status(CREATED).body(mapToOrderDto(order));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<List<OrderDto>> getCustomerOrders(@PathVariable UUID id) {
        log.info("Received request to get orders by customerId: {}", id);
        final List<Order> customerOrders = orderService.findByCustomerId(id);
        final List<OrderDto> orders = customerOrders.stream().map(Mapper::mapToOrderDto).toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID id) {
        log.info("Received request to get order: {}", id);
        final Optional<Order> order = orderService.findByOrderId(id);
        log.info("Found order {}", order);
        return order.map(o -> ResponseEntity.ok(mapToOrderDto(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderDto> deleteOrder(@PathVariable UUID id) {
        log.info("Received request to delete order {}", id);
        final int rows = orderService.deleteByOrderId(id);
        if (rows == 1) {
            log.info("Deleted order {}", id);
        } else {
            log.info("Deleted {} rows with orderId: {}", rows, id);
        }
        return ResponseEntity.noContent().build();
    }
}
