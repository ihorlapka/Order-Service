package com.electronics.store.order_service.rabbit;

import com.electronics.store.order_service.metrics.OrderServiceMetrics;
import com.electronics.store.order_service.processor.SagaProcessor;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqConsumer {

    private final SagaProcessor sagaProcessor;
    private final OrderServiceMetrics metrics;

    @RabbitListener(id = "orders-status",
            queues = "#{@rabbitMqProperties.getSagaEventsQueue()}",
            concurrency = "${app.rabbit.orders.concurrency:2-8}",
            ackMode = "AUTO")
    public void handleMessage(MessageEvent event,
                              @Header(value = AmqpHeaders.MESSAGE_ID) String eventId,
                              @Header(value = AmqpHeaders.CORRELATION_ID) String orderId,
                              @Header(value = AmqpHeaders.TIMESTAMP) long sentTimestamp) {
        try {
            log.info("Received: {}, msgId: {}, orderId: {}, sentTime: {}", event, eventId, orderId, sentTimestamp);
            var sample = metrics.startSagaProcessingTimer();
            try {
                sagaProcessor.process(event);
                metrics.incrementRabbitMessagesConsumed();
            } finally {
                metrics.stopSagaProcessingTimer(sample);
            }
        } catch (NullPointerException | IllegalArgumentException | IllegalStateException | ArrayIndexOutOfBoundsException e) {
            metrics.incrementRabbitMessagesRejected();
            throw new AmqpRejectAndDontRequeueException("Invalid event " + event, e); // straight to Dead Letter Queue
        }
    }
}
