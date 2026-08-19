package com.electronics.store.order_service.controllers;

import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.ItemService;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.validation.ItemValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;
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

        if (itemValidator.isValid(request.orderItems(), itemsByIds)) {
            final Order order = orderService.persist(request, itemsByIds);

        }
        return ResponseEntity.status(NOT_FOUND).build();
    }
}
