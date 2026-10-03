package com.electronics.store.order_service.rabbit;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(RabbitMqProperties.class)
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitMqProperties rabbitProps;


    //publish
    @Bean
    public TopicExchange createdOrdersExchange() {
        return new TopicExchange(rabbitProps.getOrdersExchange());
    }

    //listen
    @Bean
    public Queue sagaEventsQueue() {
        return QueueBuilder.durable(rabbitProps.getSagaEventsQueue())
                .quorum()
                .build();
    }

    @Bean
    public TopicExchange inventoryEventsExchange() {
        return new TopicExchange(rabbitProps.getInventoryEventsExchange());
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(rabbitProps.getPaymentEventsExchange());
    }

    @Bean
    public TopicExchange shipmentEventsExchange() {
        return new TopicExchange(rabbitProps.getShipmentEventsExchange());
    }

    @Bean
    public Binding inventoryBinding(Queue sagaEventsQueue, TopicExchange inventoryEventsExchange) {
        return BindingBuilder.bind(sagaEventsQueue)
                .to(inventoryEventsExchange)
                .with(rabbitProps.getInventoryRoutingKey());
    }

    @Bean
    public Binding paymentBinding(Queue sagaEventsQueue, TopicExchange paymentEventsExchange) {
        return BindingBuilder.bind(sagaEventsQueue)
                .to(paymentEventsExchange)
                .with(rabbitProps.getPaymentRoutingKey());
    }

    @Bean
    public Binding shipmentBinding(Queue sagaEventsQueue, TopicExchange shipmentEventsExchange) {
        return BindingBuilder.bind(sagaEventsQueue)
                .to(shipmentEventsExchange)
                .with(rabbitProps.getShipmentRoutingKey());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
