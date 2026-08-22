package com.electronics.store.order_service.controllers;

import com.electronics.store.order_service.controllers.dto.OrderDto;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.grpc.ItemService;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.mapping.Mapper;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.validation.ItemValidator;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private ItemValidator itemValidator;

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();

    private Order buildOrder() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setItemId(ITEM_ID);
        orderItem.setDescription("Test item");
        orderItem.setQuantity(2);
        orderItem.setPrice(BigDecimal.valueOf(100));
        orderItem.setImageData(null);
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
        Item item = new Item(ITEM_ID, "Test item", 2, BigDecimal.valueOf(100), null, "http://example.com/item.png");
        Order order = buildOrder();
        OrderDto expectedDto = Mapper.mapToOrderDto(order);

        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(ITEM_ID, item));
        when(itemValidator.isValid(any(), any())).thenReturn(true);
        when(orderService.persist(any(), any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedDto)));

        verify(orderService).persist(any(), any());
    }

    @Test
    void createOrder_shouldReturn404_whenItemValidationFails() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(REQUEST_ID, CUSTOMER_ID, UAH, Set.of(new RequestItem(ITEM_ID, 2)));
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of());
        when(itemValidator.isValid(any(), any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(orderService, times(0)).persist(any(), any());
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
        OrderDto expectedDto = Mapper.mapToOrderDto(order);
        when(orderService.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(order));

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
        OrderDto expectedDto = Mapper.mapToOrderDto(order);
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
        when(orderService.deleteByOrderId(orderId)).thenReturn(1);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOrder_shouldReturn204_evenWhenNoRowsDeleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.deleteByOrderId(orderId)).thenReturn(0);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isNoContent());
    }

}