package com.electronics.store.order_service.persistence.service;

import com.electronics.store.order_service.OrderServiceApplication;
import com.electronics.store.order_service.controllers.dto.RequestItem;
import com.electronics.store.order_service.controllers.misc.CreateOrderRequest;
import com.electronics.store.order_service.grpc.Item;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.model.Order;
import com.electronics.store.order_service.persistence.repositories.OrderEventRepository;
import com.electronics.store.order_service.persistence.repositories.OrderRepository;
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

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {
        OrderServiceApplication.class,
        OrderRepository.class,
        OrderService.class,
        OrderEventRepository.class,
        OrderEventService.class,
        OrderServiceIntegrationalTest.TestPersistenceConfig.class
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
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventService orderEventService;


    private CreateOrderRequest buildRequest(UUID customerId, Item... items) {
        Set<RequestItem> requestItems = Stream.of(items)
                .map(item -> new RequestItem(item.id(), 2))
                .collect(Collectors.toSet());
        return new CreateOrderRequest(UUID.randomUUID(), customerId, UAH, requestItems);
    }

    private Item buildItem(BigDecimal price) {
        return new Item(UUID.randomUUID(), "Test item", 10, price, null, "http://example.com/item.png");
    }

    @Test
    void persist_shouldSaveOrderWithItemsAndCreateOrderEvent() {
        Item item1 = buildItem(BigDecimal.valueOf(100));
        Item item2 = buildItem(BigDecimal.valueOf(50));
        Map<UUID, Item> itemsByIds = Map.of(item1.id(), item1, item2.id(), item2);
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = buildRequest(customerId, item1, item2);

        Order savedOrder = orderService.persist(request, itemsByIds);

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
        Order savedOrder = orderService.persist(request, Map.of(item.id(), item));

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

        Order ownOrder = orderService.persist(buildRequest(customerId, item), Map.of(item.id(), item));
        orderService.persist(buildRequest(UUID.randomUUID(), item), Map.of(item.id(), item));

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
    void deleteByOrderId_shouldRemoveOrderAndReturnOne() {
        Item item = buildItem(BigDecimal.valueOf(20));
        Order savedOrder = orderService.persist(buildRequest(UUID.randomUUID(), item), Map.of(item.id(), item));

        int rowsDeleted = orderService.deleteByOrderId(savedOrder.getId());

        assertThat(rowsDeleted).isEqualTo(1);
        assertThat(orderRepository.findById(savedOrder.getId())).isEmpty();
    }

    @Test
    void deleteByOrderId_shouldReturnZero_whenOrderDoesNotExist() {
        int rowsDeleted = orderService.deleteByOrderId(UUID.randomUUID());

        assertThat(rowsDeleted).isEqualTo(0);
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.electronics.store.order_service.persistence.repositories")
    @EntityScan(basePackages = "com.electronics.store.order_service.persistence.model")
    static class TestPersistenceConfig {}
}