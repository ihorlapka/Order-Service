package com.electronics.store.order_service.rabbit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(RabbitMqProperties.PROPERTIES_PREFIX)
@RequiredArgsConstructor
public class RabbitMqProperties {

    final static String PROPERTIES_PREFIX = "rabbit";

    //publish
    @Value("${" + PROPERTIES_PREFIX + ".orders.exchange.name}")
    private String ordersExchange;

    @Value("${" + PROPERTIES_PREFIX + ".orders.routing.key.created}")
    private String orderCreatedRoutingKey;

    @Value("${" + PROPERTIES_PREFIX + ".orders.routing.key.modified}")
    private String orderModifiedRoutingKey;

    @Value("${" + PROPERTIES_PREFIX + ".orders.routing.key.cancelled}")
    private String orderCancelledRoutingKey;


    //listen
    @Value("${" + PROPERTIES_PREFIX + ".saga.queue.name}")
    private String sagaEventsQueue;

    @Value("${" + PROPERTIES_PREFIX + ".inventory.exchange.name}")
    private String inventoryEventsExchange;

    @Value("${" + PROPERTIES_PREFIX + ".payment.exchange.name}")
    private String paymentEventsExchange;

    @Value("${" + PROPERTIES_PREFIX + ".shipment.exchange.name}")
    private String shipmentEventsExchange;

    @Value("${" + PROPERTIES_PREFIX + ".inventory.routing.key}")
    private String inventoryRoutingKey;

    @Value("${" + PROPERTIES_PREFIX + ".payment.routing.key}")
    private String paymentRoutingKey;

    @Value("${" + PROPERTIES_PREFIX + ".shipment.routing.key}")
    private String shipmentRoutingKey;
}
