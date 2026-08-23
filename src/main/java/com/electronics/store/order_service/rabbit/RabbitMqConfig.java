package com.electronics.store.order_service.rabbit;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitMqProperties rabbitMqProperties;

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(rabbitMqProperties.getQueueName())
                .quorum()
                .build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(rabbitMqProperties.getExchange());
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue())
                .to(topicExchange())
                .with(rabbitMqProperties.getRoutingKey());
    }
}
