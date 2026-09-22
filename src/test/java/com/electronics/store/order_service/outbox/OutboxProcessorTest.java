package com.electronics.store.order_service.outbox;

import com.electronics.store.order_service.OrderServiceApplication;
import com.electronics.store.order_service.persistence.enums.PublishmentStatus;
import com.electronics.store.order_service.persistence.enums.OutboxEventType;
import com.electronics.store.order_service.persistence.model.OutboxEvent;
import com.electronics.store.order_service.persistence.repositories.OutboxEventRepository;
import com.electronics.store.order_service.persistence.service.OutboxEventService;
import com.electronics.store.order_service.rabbit.RabbitMqPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(classes = {
        OrderServiceApplication.class,
        OutboxProcessor.class,
        RabbitMqPublisher.class,
        OutboxEventService.class,
        OutboxEventRepository.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.yaml")
class OutboxProcessorTest {

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
    private OutboxProcessor outboxProcessor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private RabbitMqPublisher rabbitMqPublisher;

    @BeforeEach
    void cleanUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void processBatch_publishesAllNewEvents_andMarksThemPublished() {
        saveNewEvent();
        saveNewEvent();

        OutboxProcessor.ProcessingResult result = outboxProcessor.processBatch();

        assertThat(result.hasMore()).isTrue();
        assertThat(result.hasError()).isFalse();

        verify(rabbitMqPublisher, times(2)).publish(any(UUID.class), anyString());

        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getStatus)
                .containsOnly(PublishmentStatus.PUBLISHED);
    }

    @Test
    void processBatch_returnsNoMore_whenTableHasNoNewEvents() {
        OutboxProcessor.ProcessingResult result = outboxProcessor.processBatch();

        assertThat(result.hasMore()).isFalse();
        assertThat(result.hasError()).isFalse();
        verifyNoInteractions(rabbitMqPublisher);
    }

    @Test
    void processBatch_leavesEventAsNew_whenPublishFails() {
        OutboxEvent event = saveNewEvent();
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitMqPublisher).publish(any(UUID.class), anyString());

        OutboxProcessor.ProcessingResult result = outboxProcessor.processBatch();

        assertThat(result.hasMore()).isTrue();
        assertThat(result.hasError()).isTrue();

        OutboxEvent reloaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PublishmentStatus.NEW);
    }

    @Test
    void processBatch_publishesSuccessfulEventsOnly_whenOneEventFailsInBatch() {
        OutboxEvent ok1 = saveNewEvent();
        OutboxEvent failing = saveNewEvent();
        OutboxEvent ok2 = saveNewEvent();

        doAnswer(invocation -> {
            UUID orderId = invocation.getArgument(0);
            if (orderId.equals(failing.getOrderId())) {
                throw new AmqpException("simulated failure for one event");
            }
            return null;
        }).when(rabbitMqPublisher).publish(any(UUID.class), anyString());

        OutboxProcessor.ProcessingResult result = outboxProcessor.processBatch();

        assertThat(result.hasError()).isTrue();
        assertThat(outboxEventRepository.findById(ok1.getId()).orElseThrow().getStatus())
                .isEqualTo(PublishmentStatus.PUBLISHED);
        assertThat(outboxEventRepository.findById(ok2.getId()).orElseThrow().getStatus())
                .isEqualTo(PublishmentStatus.PUBLISHED);
        assertThat(outboxEventRepository.findById(failing.getId()).orElseThrow().getStatus())
                .isEqualTo(PublishmentStatus.NEW);
    }

    /**
     * The whole reason SKIP LOCKED was introduced: two concurrent
     * transactions calling processBatch() at the same time must never both
     * grab the same row. 250 events (more than the hardcoded batch size of
     * 100) guarantees both worker threads have real work to contend over.
     */
    @Test
    void processBatch_neverPublishesTheSameEventTwice_whenRunConcurrently() throws Exception {
        int totalEvents = 250;
        List<UUID> expectedOrderIds = new ArrayList<>();
        for (int i = 0; i < totalEvents; i++) {
            expectedOrderIds.add(saveNewEvent().getOrderId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Void> drainAllBatches = () -> {
            startLatch.await();
            boolean hasMore = true;
            while (hasMore) {
                hasMore = outboxProcessor.processBatch().hasMore();
            }
            return null;
        };

        Future<Void> worker1 = pool.submit(drainAllBatches);
        Future<Void> worker2 = pool.submit(drainAllBatches);
        startLatch.countDown();

        worker1.get(30, TimeUnit.SECONDS);
        worker2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        ArgumentCaptor<UUID> publishedOrderIds = ArgumentCaptor.forClass(UUID.class);
        verify(rabbitMqPublisher, times(totalEvents)).publish(publishedOrderIds.capture(), anyString());

        // every event published exactly once, none skipped, none duplicated
        assertThat(publishedOrderIds.getAllValues())
                .containsExactlyInAnyOrderElementsOf(expectedOrderIds);

        assertThat(outboxEventRepository.findAll())
                .extracting(OutboxEvent::getStatus)
                .containsOnly(PublishmentStatus.PUBLISHED);
    }

    private OutboxEvent saveNewEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setEventType(OutboxEventType.ORDER_CREATED);
        event.setPayload("{\"message\":\"hello\"}");
        event.setStatus(PublishmentStatus.NEW);
        return outboxEventRepository.save(event);
    }

}