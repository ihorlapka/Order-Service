package com.electronics.store.order_service.controllers;

import com.electronics.store.order_service.controllers.dto.DtoMapper;
import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.persistence.service.exceptions.NotEnoughItemsException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderCancellationIsNotAllowedException;
import com.electronics.store.order_service.persistence.service.exceptions.OutboxEventNotFoundException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderIsAlreadyCancelledException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static com.electronics.store.order_service.persistence.enums.Currency.UAH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();

    private Order buildOrder() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setItemId(ITEM_ID);
        orderItem.setQuantity(2);
        orderItem.setPrice(BigDecimal.valueOf(100));
        orderItem.setItemUrl("http://example.com/item.png");

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(CUSTOMER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        order.setCurrency(UAH);
        order.setTotalPrice(BigDecimal.valueOf(200));
        order.setItems(Set.of(orderItem));

        return order;
    }

    @Test
    void createOrder_shouldReturn201_whenRequestIsValid() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(REQUEST_ID, CUSTOMER_ID, UAH, Set.of(new RequestItem(ITEM_ID, 2)));
        Order order = buildOrder();
        OrderDto expectedDto = DtoMapper.mapToOrderDto(order);

        when(orderService.persist(any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedDto)));

        verify(orderService).persist(any());
    }

    @Test()
    void createOrder_shouldReturn404_whenItemValidationFails() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(REQUEST_ID, CUSTOMER_ID, UAH, Set.of(new RequestItem(ITEM_ID, 2)));
        when(orderService.persist(any())).thenThrow(new NotEnoughItemsException("Not enough items in inventory!"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Not enough items in inventory!"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders"));

        verify(orderService, times(1)).persist(any());
    }

    @Test
    void returns400_withFieldErrors_whenRequestBodyFailsBeanValidation() throws Exception {
        String invalidBody = """
                    {
                        "requestId": null
                        "customerId": null,
                        "currency": null,
                        "orderItems": []
                    }
                    """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void returns400_whenRequestBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content("{ not-valid-json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void returns500_whenServiceThrowsUnexpectedException() throws Exception {
        when(orderService.persist(any())).thenThrow(new RuntimeException("db is on fire"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(validCreateOrderRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"));
    }

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Test
    void returns200_whenOrderExists() throws Exception {
        Order order = buildOrder();
        order.setId(ORDER_ID);
        when(orderService.findByOrderId(ORDER_ID)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void returns404_whenOrderDoesNotExist() throws Exception {
        when(orderService.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400_whenIdPathVariableIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'not-a-uuid' for parameter 'id'"));
    }

    @Test
    void returns204_whenCancelledSuccessfully() throws Exception {
        doNothing().when(orderService).cancelOrder(ORDER_ID);

        mockMvc.perform(delete("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void returns404_whenOutboxEventNotFound() throws Exception {
        doThrow(new OutboxEventNotFoundException("Outbox event with orderId: " + ORDER_ID + " not found!"))
                .when(orderService).cancelOrder(ORDER_ID);

        mockMvc.perform(delete("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Outbox event with orderId: " + ORDER_ID + " not found!"));
    }

    @Test
    void returns409_whenOrderIsAlreadyCancelled() throws Exception {
        doThrow(new OrderIsAlreadyCancelledException("Outbox event with orderId: " + ORDER_ID + " is cancelled"))
                .when(orderService).cancelOrder(ORDER_ID);

        mockMvc.perform(delete("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void returns422_whenCancellationNotAllowedBecauseAlreadyShipped() throws Exception {
        doThrow(new OrderCancellationIsNotAllowedException(
                "Unable to cancel outbox event with orderId: " + ORDER_ID + " because shipment has already been started"))
                .when(orderService).cancelOrder(ORDER_ID);

        mockMvc.perform(delete("/api/v1/orders/{id}", ORDER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        "Unable to cancel outbox event with orderId: " + ORDER_ID +
                                " because shipment has already been started"));
    }

    @Test
    void createOrder_shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCustomerOrders_shouldReturnOrdersList() throws Exception {
        Order order = buildOrder();
        OrderDto expectedDto = DtoMapper.mapToOrderDto(order);
        when(orderService.findByCustomerId(OrderControllerTest.CUSTOMER_ID)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/orders/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(expectedDto))));
    }

    @Test
    void getCustomerOrders_shouldReturnEmptyList_whenNoOrdersFound() throws Exception {
        when(orderService.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders/customers/{id}", CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getOrder_shouldReturn200_whenOrderExists() throws Exception {
        Order order = buildOrder();
        OrderDto expectedDto = DtoMapper.mapToOrderDto(order);
        when(orderService.findByOrderId(order.getId())).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/v1/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedDto)));
    }

    @Test
    void getOrder_shouldReturn404_whenOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.findByOrderId(orderId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_shouldReturn204_whenOrderDeleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOrder_shouldReturn204_evenWhenNoRowsDeleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());
    }

    private String validCreateOrderRequestJson() throws Exception {
        return """
                {
                    "requestId": "%s",
                    "customerId": "%s",
                    "currency": "USD",
                    "requestItems": [
                        { "itemId": "%s", "quantity": 2 }
                    ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

}