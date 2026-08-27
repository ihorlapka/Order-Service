package com.electronics.store.order_service.outbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventManagerTest {

    @Mock
    private OutboxProcessor outboxProcessor;

    @InjectMocks
    private OutboxEventManager outboxEventManager;

    @AfterEach
    void tearDown() throws InterruptedException {
        outboxEventManager.destroy();
    }

    @Test
    void publishAndUpdate_loopsUntilNoMoreEvents() {
        when(outboxProcessor.processBatch())
                .thenReturn(new OutboxProcessor.ProcessingResult(true, false))
                .thenReturn(new OutboxProcessor.ProcessingResult(true, false))
                .thenReturn(new OutboxProcessor.ProcessingResult(false, false));

        outboxEventManager.publishAndUpdate();

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(3)).processBatch());
    }

    @Test
    void publishAndUpdate_ignoresSecondTrigger_whileFirstRunStillInProgress() throws InterruptedException {
        CountDownLatch firstBatchStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstBatch = new CountDownLatch(1);

        when(outboxProcessor.processBatch()).thenAnswer(invocation -> {
            firstBatchStarted.countDown();
            // block "inside" the first run so a concurrent trigger arrives
            // while processing=true
            assertThat(releaseFirstBatch.await(2, TimeUnit.SECONDS)).isTrue();
            return new OutboxProcessor.ProcessingResult(false, false);
        });

        outboxEventManager.publishAndUpdate();
        assertThat(firstBatchStarted.await(1, TimeUnit.SECONDS))
                .as("first run should have started")
                .isTrue();

        // second trigger while processing is still true -> must be a no-op
        outboxEventManager.publishAndUpdate();

        releaseFirstBatch.countDown();

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(1)).processBatch());
    }

    @Test
    void publishAndUpdate_allowsNewRun_oncePreviousRunFinished() {
        when(outboxProcessor.processBatch())
                .thenReturn(new OutboxProcessor.ProcessingResult(false, false));

        outboxEventManager.publishAndUpdate();
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(1)).processBatch());

        outboxEventManager.publishAndUpdate();
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(2)).processBatch());
    }

    @Test
    void publishAndUpdate_releasesGuard_evenWhenProcessorThrows() {
        when(outboxProcessor.processBatch()).thenThrow(new RuntimeException("boom"));

        outboxEventManager.publishAndUpdate();
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(1)).processBatch());

        outboxEventManager.publishAndUpdate();
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(2)).processBatch());
    }

    @Test
    void publishAndUpdate_backsOffFiveSeconds_beforeRetryingAfterBatchError() {
        when(outboxProcessor.processBatch())
                .thenReturn(new OutboxProcessor.ProcessingResult(true, true))
                .thenReturn(new OutboxProcessor.ProcessingResult(false, false));

        long start = System.currentTimeMillis();
        outboxEventManager.publishAndUpdate();

        await().atMost(7, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(2)).processBatch());

        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed)
                .as("the 5s sleep on hasError=true must have actually happened")
                .isGreaterThanOrEqualTo(5000);
    }

    @Test
    void destroy_shutsDownExecutorGracefully() throws InterruptedException {
        when(outboxProcessor.processBatch())
                .thenReturn(new OutboxProcessor.ProcessingResult(false, false));

        outboxEventManager.publishAndUpdate();
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(outboxProcessor, times(1)).processBatch());

        outboxEventManager.destroy();
    }
}