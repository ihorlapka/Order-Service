package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.OrderServiceApplication;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.controllers.misc.UpdateOrderRequest;
import com.electronics.store.order_service.inventory.InventoryResponse;
import com.electronics.store.order_service.inventory.Item;
import com.electronics.store.order_service.inventory.ItemService;
import com.electronics.store.order_service.outbox.OutboxEventManager;
import com.electronics.store.order_service.persistence.enums.OrderEventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OrderEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.repositories.OrderEventRepository;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import com.electronics.store.order_service.persistence.service.exceptions.InventoryNotAvailableException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderIsAlreadyCancelledException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderNotFoundException;
import com.electronics.store.order_service.persistence.service.exceptions.UpdateRequestIsNotValidException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.electronics.store.order_service.persistence.enums.Currency.UAH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {
        OrderServiceApplication.class,
        OrderRepository.class,
        OrderService.class,
        OrderEventRepository.class,
        OrderEventService.class,
        OrderServiceIntegrationalTest.TestPersistenceConfig.class,
        OutboxEventManager.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@TestPropertySource("classpath:application-test.yaml")
class OrderServiceIntegrationalTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.5"))
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderEventService orderEventService;
    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ItemService itemService;


    private CreateOrderRequest buildRequest(UUID customerId, Item... items) {
        Set<RequestItem> requestItems = Stream.of(items)
                .map(item -> new RequestItem(item.id(), 2))
                .collect(Collectors.toSet());
        return new CreateOrderRequest(UUID.randomUUID(), customerId, UAH, requestItems);
    }

    private Item buildItem(BigDecimal price) {
        return new Item(UUID.randomUUID(), "Test item", true, 10, price, null, "http://example.com/item.png");
    }

    @Test
    void persist_shouldSaveOrderWithItemsAndCreateOrderEvent() {
        Item item1 = buildItem(BigDecimal.valueOf(100));
        Item item2 = buildItem(BigDecimal.valueOf(50));
        Map<UUID, Item> itemsByIds = Map.of(item1.id(), item1, item2.id(), item2);
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = buildRequest(customerId, item1, item2);
        when(itemService.getItemsByIds(anySet())).thenReturn(itemsByIds);

        Order savedOrder = orderService.persist(request);

        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(savedOrder.getCurrency()).isEqualTo(UAH);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(savedOrder.getItems()).hasSize(2);

        Optional<Order> persisted = orderService.findByOrderId(savedOrder.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getItems()).hasSize(2);

        assertThat(orderEventService.findByOrderId(savedOrder.getId())).isPresent();
    }

    @Test
    void findByOrderId_shouldReturnOrder_whenOrderExists() {
        Item item = buildItem(BigDecimal.valueOf(75));
        CreateOrderRequest request = buildRequest(UUID.randomUUID(), item);
        Map<UUID, Item> itemsById = Map.of(item.id(), item);
        when(itemService.getItemsByIds(anySet())).thenReturn(itemsById);
        Order savedOrder = orderService.persist(request);

        Optional<Order> found = orderService.findByOrderId(savedOrder.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedOrder.getId());
    }

    @Test
    void findByOrderId_shouldReturnEmpty_whenOrderDoesNotExist() {
        Optional<Order> found = orderService.findByOrderId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findByCustomerId_shouldReturnOnlyOrdersOfThatCustomer() {
        UUID customerId = UUID.randomUUID();
        Item item = buildItem(BigDecimal.valueOf(30));

        Map<UUID, Item> itemsById = Map.of(item.id(), item);
        when(itemService.getItemsByIds(anySet())).thenReturn(itemsById);
        Order ownOrder = orderService.persist(buildRequest(customerId, item));
        orderService.persist(buildRequest(UUID.randomUUID(), item));

        List<Order> customerOrders = orderService.findByCustomerId(customerId);

        assertThat(customerOrders)
                .extracting(Order::getId)
                .containsExactly(ownOrder.getId());
    }

    @Test
    void findByCustomerId_shouldReturnEmptyList_whenCustomerHasNoOrders() {
        List<Order> customerOrders = orderService.findByCustomerId(UUID.randomUUID());

        assertThat(customerOrders).isEmpty();
    }

    @Test
    void reservesItems_andSetsStatusReserved_whenReservationSucceeds() {
        Item existingItem = buildItem(BigDecimal.valueOf(20), true);
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(existingItem.id(), existingItem));
        Order order = orderService.persist(buildRequest(UUID.randomUUID(), existingItem));

        Item newItem = buildItem(BigDecimal.valueOf(40), true);
        InventoryResponse reserveResponse = new InventoryResponse(true, "reserved", Map.of(newItem.id(), newItem));
        when(itemService.reserve(anySet())).thenReturn(reserveResponse);

        UpdateOrderRequest request = buildAddRequest(order.getId(), newItem);
        Order patched = orderService.patch(request);

        assertThat(patched.getStatus()).isEqualTo(OrderStatus.RESERVED);
        assertThat(patched.getItems())
                .extracting(OrderItem::getItemId)
                .contains(newItem.id());

        List<OrderEvent> events = orderEventService.findAllByOrderId(order.getId());
        assertThat(events)
                .extracting(OrderEvent::getEventType)
                .contains(OrderEventType.INVENTORY_RESERVED);
    }

    @Test
    void marksOrderReservationFailed_andSkipsUnreservedItems_whenReservationFails() {
        Item existingItem = buildItem(BigDecimal.valueOf(20), true);
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(existingItem.id(), existingItem));
        Order order = orderService.persist(buildRequest(UUID.randomUUID(), existingItem));

        Item unreservedItem = buildItem(BigDecimal.valueOf(40), false);
        InventoryResponse reserveResponse = new InventoryResponse(
                false, "not enough stock", Map.of(unreservedItem.id(), unreservedItem));
        when(itemService.reserve(anySet())).thenReturn(reserveResponse);

        UpdateOrderRequest request = buildAddRequest(order.getId(), unreservedItem);
        Order patched = orderService.patch(request);

        assertThat(patched.getStatus()).isEqualTo(OrderStatus.RESERVATION_FAILED);
        // the not-reserved item must NOT have been added to the order
        assertThat(patched.getItems())
                .extracting(OrderItem::getItemId)
                .doesNotContain(unreservedItem.id());

        List<OrderEvent> events = orderEventService.findAllByOrderId(order.getId());
        assertThat(events)
                .extracting(OrderEvent::getEventType)
                .contains(OrderEventType.INVENTORY_FAILED);
    }

    @Test
    void removesItems_andReleasesInventory_whenRemovalSucceeds() {
        Item item = buildItem(BigDecimal.valueOf(20), true);
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(item.id(), item));
        Order order = orderService.persist(buildRequest(UUID.randomUUID(), item));
        assertThat(order.getItems()).hasSize(1);

        when(itemService.release(anySet())).thenReturn(new InventoryResponse(true, "released", Map.of()));

        UpdateOrderRequest request = buildRemoveRequest(order.getId(), Set.of(item.id()));
        Order patched = orderService.patch(request);

        assertThat(patched.getItems())
                .extracting(OrderItem::getItemId)
                .doesNotContain(item.id());
    }

    @Test
    void throwsInventoryNotAvailable_whenReleaseCallFails() {
        Item item = buildItem(BigDecimal.valueOf(20), true);
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(item.id(), item));
        Order order = orderService.persist(buildRequest(UUID.randomUUID(), item));

        when(itemService.release(anySet()))
                .thenReturn(new InventoryResponse(false, "inventory service unreachable", Map.of()));

        UpdateOrderRequest request = buildRemoveRequest(order.getId(), Set.of(item.id()));

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(InventoryNotAvailableException.class);
    }

    @Test
    void throwsOrderNotFound_whenOrderDoesNotExist() {
        Item item = buildItem(BigDecimal.valueOf(20), true);
        UpdateOrderRequest request = buildAddRequest(UUID.randomUUID(), item);

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void throwsUpdateRequestIsNotValid_whenBothItemSetsAreEmpty() {
        UpdateOrderRequest request = new UpdateOrderRequest(UUID.randomUUID(), UUID.randomUUID(), Set.of(), Set.of());

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(UpdateRequestIsNotValidException.class);
    }

    @Test
    void throwsOrderIsAlreadyCancelled_whenOrderStatusIsCancelled() {
        Item item = buildItem(BigDecimal.valueOf(20), true);
        when(itemService.getItemsByIds(anySet())).thenReturn(Map.of(item.id(), item));
        Order order = orderService.persist(buildRequest(UUID.randomUUID(), item));
        forceStatus(order.getId(), OrderStatus.CANCELLED);

        UpdateOrderRequest request = buildAddRequest(order.getId(), item);

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(OrderIsAlreadyCancelledException.class);
    }

    private Item buildItem(BigDecimal price, boolean reserved) {
        return new Item(UUID.randomUUID(), "Test item", reserved, 10, price, null,
                "http://example.com/item.png");
    }

    private UpdateOrderRequest buildAddRequest(UUID orderId, Item... items) {
        Set<RequestItem> requestItems = Stream.of(items)
                .map(item -> new RequestItem(item.id(), 2))
                .collect(Collectors.toSet());
        return new UpdateOrderRequest(UUID.randomUUID(), orderId, requestItems, Set.of());
    }

    private UpdateOrderRequest buildRemoveRequest(UUID orderId, Set<UUID> itemIdsToRemove) {
        Set<RequestItem> requestItems = itemIdsToRemove.stream()
                .map(id -> new RequestItem(id, 1))
                .collect(Collectors.toSet());
        return new UpdateOrderRequest(UUID.randomUUID(), orderId, Set.of(), requestItems);
    }

    private Order forceStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        return orderRepository.save(order);
    }


    @Configuration
    @EnableJpaRepositories(basePackages = "com.electronics.store.order_service.persistence.repositories")
    @EntityScan(basePackages = "com.electronics.store.order_service.persistence.model")
    static class TestPersistenceConfig {}
}