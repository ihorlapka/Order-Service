package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.OrderServiceApplication;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.controllers.misc.UpdateOrderRequest;
import com.electronics.store.order_service.outbox.OutboxEventManager;
import com.electronics.store.order_service.persistence.enums.OutboxEventType;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.enums.PublishmentStatus;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.model.OrderItem;
import com.electronics.store.order_service.persistence.repositories.OutboxEventRepository;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
import com.electronics.store.order_service.persistence.service.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.electronics.store.order_service.persistence.enums.Currency.UAH;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {
        OrderServiceApplication.class,
        OrderRepository.class,
        OrderService.class,
        OutboxEventRepository.class,
        OutboxEventService.class,
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
    private OutboxEventService outboxEventService;

    @Autowired
    private OrderRepository orderRepository;

    private CreateOrderRequest buildCreateRequest(UUID customerId, UUID... itemIds) {
        Set<RequestItem> items = Stream.of(itemIds)
                .map(id -> new RequestItem(id, 2))
                .collect(Collectors.toSet());
        return new CreateOrderRequest(UUID.randomUUID(), customerId, UAH, items);
    }

    private UpdateOrderRequest buildUpdateRequest(UUID orderId, UUID... itemIds) {
        Set<RequestItem> items = Stream.of(itemIds)
                .map(id -> new RequestItem(id, 3))
                .collect(Collectors.toSet());
        return new UpdateOrderRequest(UUID.randomUUID(), orderId, items);
    }

    private void markOrderCreatedEventPublished(UUID orderId) {
        OutboxEvent event = outboxEventService.findLastByOrderIdForUpdate(orderId).orElseThrow();
        event.setStatus(PublishmentStatus.PUBLISHED);
        outboxEventService.persist(event);
    }

    private void insertOutboxEvent(UUID orderId, OutboxEventType type, PublishmentStatus status) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType(type);
        event.setOrderId(orderId);
        event.setCreatedAt(now());
        event.setPayload("{}");
        event.setStatus(status);
        outboxEventService.persist(event);
    }

    private Order forceStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Test
    void savesOrder_andCreatesOrderCreatedOutboxEvent() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = buildCreateRequest(customerId, UUID.randomUUID(), UUID.randomUUID());

        Order saved = orderService.persist(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(orderService.findByOrderId(saved.getId())).isPresent();

        Optional<OutboxEvent> event = outboxEventService.findLastByOrderIdForUpdate(saved.getId());
        assertThat(event).isPresent();
        assertThat(event.get().getEventType()).isEqualTo(OutboxEventType.ORDER_CREATED);
        assertThat(event.get().getStatus()).isEqualTo(PublishmentStatus.NEW);
    }

    @Test
    void findByOrderId_returnsEmpty_whenOrderDoesNotExist() {
        assertThat(orderService.findByOrderId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByCustomerId_returnsOnlyThatCustomersOrders() {
        UUID customerId = UUID.randomUUID();
        Order own = orderService.persist(buildCreateRequest(customerId, UUID.randomUUID()));
        orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));

        List<Order> result = orderService.findByCustomerId(customerId);

        assertThat(result).extracting(Order::getId).containsExactly(own.getId());
    }

    @Test
    void findByCustomerId_returnsEmptyList_whenCustomerHasNoOrders() {
        assertThat(orderService.findByCustomerId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void hardDeletesOrderAndEvent_whenOrderCreatedEventIsStillNew() {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));

        orderService.cancelOrder(order.getId());

        assertThat(orderService.findByOrderId(order.getId())).isEmpty();
        assertThat(outboxEventService.findLastByOrderIdForUpdate(order.getId())).isEmpty();
    }

    @Test
    void softCancels_marksOrderCancelledAndAddsCancelledEvent_whenOrderCreatedAlreadyPublished() {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));
        markOrderCreatedEventPublished(order.getId());

        orderService.cancelOrder(order.getId());

        Order reloaded = orderService.findByOrderId(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        OutboxEvent latest = outboxEventService.findLastByOrderIdForUpdate(order.getId()).orElseThrow();
        assertThat(latest.getEventType()).isEqualTo(OutboxEventType.ORDER_CANCELLED);
        assertThat(latest.getStatus()).isEqualTo(PublishmentStatus.NEW);
    }

    @Test
    void throwsAlreadyCancelled_whenCancelledTwice() {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));
        markOrderCreatedEventPublished(order.getId());
        orderService.cancelOrder(order.getId());

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(OrderIsAlreadyCancelledException.class);
    }

    @Test
    void throwsCancellationNotAllowed_whenShipmentAlreadyStarted() {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));
        markOrderCreatedEventPublished(order.getId());
        insertOutboxEvent(order.getId(), OutboxEventType.SHIPMENT_CREATED, PublishmentStatus.NEW);

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(OrderCancellationIsNotAllowedException.class);
    }

    @Test
    void throwsOutboxEventNotFound_whenOrderNeverExisted() {
        assertThatThrownBy(() -> orderService.cancelOrder(UUID.randomUUID()))
                .isInstanceOf(OutboxEventNotFoundException.class);
    }

    @Test
    void throwsUpdateRequestIsNotValid_whenItemsToUpdateIsEmpty() {
        UpdateOrderRequest request = new UpdateOrderRequest(UUID.randomUUID(), UUID.randomUUID(), Set.of());

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(UpdateRequestIsNotValidException.class);
    }

    @Test
    void throwsOrderNotFound_whenOrderDoesNotExist() {
        UpdateOrderRequest request = buildUpdateRequest(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void throwsOrderIsAlreadyCancelled_whenOrderWasCancelled() {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));
        markOrderCreatedEventPublished(order.getId());
        orderService.cancelOrder(order.getId()); // soft-cancel -> status becomes CANCELLED

        UpdateOrderRequest request = buildUpdateRequest(order.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(OrderIsAlreadyCancelledException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class,
            names = {"SHIPPED", "DELIVERED", "DELIVERY_FAILED", "PENDING_PAYMENT", "PAYMENT_STUCK", "PAID"})
    void throwsOrderChangeRestricted_whenOrderIsPastModifiableStages(OrderStatus terminalStatus) {
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), UUID.randomUUID()));
        forceStatus(order.getId(), terminalStatus);

        UpdateOrderRequest request = buildUpdateRequest(order.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> orderService.patch(request))
                .isInstanceOf(OrderChangeRestrictedException.class);
    }

    @Test
    void replacesItems_setsStatusModified_andCreatesOrderModifiedEvent() {
        UUID originalItemId = UUID.randomUUID();
        Order order = orderService.persist(buildCreateRequest(UUID.randomUUID(), originalItemId));

        UUID newItemId = UUID.randomUUID();
        UpdateOrderRequest request = buildUpdateRequest(order.getId(), newItemId);

        Order patched = orderService.patch(request);

        assertThat(patched.getStatus()).isEqualTo(OrderStatus.MODIFIED);
        assertThat(patched.getItems())
                .extracting(OrderItem::getItemId)
                .containsExactly(newItemId);

        OutboxEvent latest = outboxEventService.findLastByOrderIdForUpdate(order.getId()).orElseThrow();
        assertThat(latest.getEventType()).isEqualTo(OutboxEventType.ORDER_MODIFIED);
        assertThat(latest.getStatus()).isEqualTo(PublishmentStatus.NEW);
    }


    @Configuration
    @EnableJpaRepositories(basePackages = "com.electronics.store.order_service.persistence.repositories")
    @EntityScan(basePackages = "com.electronics.store.order_service.persistence.model")
    static class TestPersistenceConfig {}
}