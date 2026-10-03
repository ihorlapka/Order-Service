package com.electronics.store.order_service.rabbit;

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

    @RabbitListener(id = "orders-updated",
            queues = "#{@rabbitMqProperties.getSagaEventsQueue()}",
            concurrency = "${app.rabbit.orders.concurrency:2-8}",
            ackMode = "AUTO")
    public void handleMessage(MessageEvent event,
                              @Header(value = AmqpHeaders.MESSAGE_ID) String messageId,
                              @Header(value = AmqpHeaders.CORRELATION_ID) String correlationId,
                              @Header(value = AmqpHeaders.TIMESTAMP) long sentTimestamp) {
        try {
            log.info("Received: {}, msgId: {}, correlationId: {}, sentTime: {}", event, messageId, correlationId, sentTimestamp);
            //todo: do something here!
        } catch (NullPointerException | IllegalArgumentException | IllegalStateException | ArrayIndexOutOfBoundsException e) {
            throw new AmqpRejectAndDontRequeueException("Invalid event " + event, e); // straight to Dead Letter Queue
        }
    }
}
