package com.electronics.store.order_service.rabbit;

import com.electronics.store.order_service.persistence.enums.Currency;
import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.enums.EventType;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import com.electronics.store.order_service.rabbit.message.OrderCancelledData;
import com.electronics.store.order_service.rabbit.message.OrderCreatedData;
import com.electronics.store.order_service.rabbit.message.OrderModifiedData;
import com.electronics.store.order_service.rabbit.message.PaymentCompletedData;
import com.electronics.store.order_service.rabbit.message.PaymentFailedData;
import com.electronics.store.order_service.rabbit.message.ShipmentCompletedData;
import com.electronics.store.order_service.rabbit.message.ShipmentCreatedData;
import com.electronics.store.order_service.rabbit.message.ShipmentFailedData;
import com.electronics.store.order_service.rabbit.message.InventoryReservedData;
import com.electronics.store.order_service.rabbit.message.InventoryFailedData;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RabbitMqConsumerDeserializationTest {

    private final JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter("com.electronics.store.order_service.rabbit.message");

    @Test
    void shouldDeserializeOrderCreatedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "ORDER_CREATED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "PENDING",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "customerId": "c3d4e5f6-a7b8-9012-cdef-345678901234",
                    "currency": "USD",
                    "totalPrice": "99.99",
                    "items": [
                        {"id": "d4e5f6a7-b8c9-0123-defa-456789012345", "itemId": "e5f6a7b8-c9d0-1234-efab-567890123456", "quantity": 2},
                        {"id": "f6a7b8c9-d0e1-2345-fabc-678901234567", "itemId": "a7b8c9d0-e1f2-3456-abcd-789012345678", "quantity": 1}
                    ]
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.ORDER_CREATED, event.eventType());
        assertEquals(OrderStatus.PENDING, event.orderStatus());
        assertInstanceOf(OrderCreatedData.class, event.eventData());
        OrderCreatedData data = (OrderCreatedData) event.eventData();
        assertEquals(Currency.USD, data.currency());
        assertEquals(2, data.items().size());
    }

    @Test
    void shouldDeserializeOrderCancelledEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "ORDER_CANCELLED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "CANCELLED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "reason": "Customer requested cancellation"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.ORDER_CANCELLED, event.eventType());
        assertEquals(OrderStatus.CANCELLED, event.orderStatus());
        assertInstanceOf(OrderCancelledData.class, event.eventData());
        OrderCancelledData data = (OrderCancelledData) event.eventData();
        assertEquals("Customer requested cancellation", data.reason());
    }

    @Test
    void shouldDeserializeOrderModifiedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "ORDER_MODIFIED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "MODIFIED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "itemsToUpdate": [
                        {"id": "d4e5f6a7-b8c9-0123-defa-456789012345", "itemId": "e5f6a7b8-c9d0-1234-efab-567890123456", "quantity": 3}
                    ]
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.ORDER_MODIFIED, event.eventType());
        assertEquals(OrderStatus.MODIFIED, event.orderStatus());
        assertInstanceOf(OrderModifiedData.class, event.eventData());
        OrderModifiedData data = (OrderModifiedData) event.eventData();
        assertEquals(1, data.itemsToUpdate().size());
    }

    @Test
    void shouldDeserializeInventoryReservedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "INVENTORY_RESERVED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "RESERVED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "reservedItems": [
                        {"itemId": "e5f6a7b8-c9d0-1234-efab-567890123456", "quantity": 2},
                        {"itemId": "a7b8c9d0-e1f2-3456-abcd-789012345678", "quantity": 1}
                    ]
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.INVENTORY_RESERVED, event.eventType());
        assertEquals(OrderStatus.RESERVED, event.orderStatus());
        assertInstanceOf(InventoryReservedData.class, event.eventData());
        InventoryReservedData data = (InventoryReservedData) event.eventData();
        assertEquals(2, data.reservedItems().size());
    }

    @Test
    void shouldDeserializeInventoryFailedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "INVENTORY_FAILED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "RESERVATION_FAILED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "unavailableItems": [
                        {"itemId": "e5f6a7b8-c9d0-1234-efab-567890123456", "requestedQuantity": 5, "availableQuantity": 2}
                    ],
                    "reason": "Insufficient stock"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.INVENTORY_FAILED, event.eventType());
        assertEquals(OrderStatus.RESERVATION_FAILED, event.orderStatus());
        assertInstanceOf(InventoryFailedData.class, event.eventData());
        InventoryFailedData data = (InventoryFailedData) event.eventData();
        assertEquals(1, data.unavailableItems().size());
        assertEquals("Insufficient stock", data.reason());
    }

    @Test
    void shouldDeserializePaymentCompletedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "PAYMENT_COMPLETED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "PAID",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "transactionId": "c3d4e5f6-a7b8-9012-cdef-345678901234",
                    "currency": "EUR",
                    "amount": "149.50",
                    "paymentMethod": "credit_card"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.PAYMENT_COMPLETED, event.eventType());
        assertEquals(OrderStatus.PAID, event.orderStatus());
        assertInstanceOf(PaymentCompletedData.class, event.eventData());
        PaymentCompletedData data = (PaymentCompletedData) event.eventData();
        assertEquals(Currency.EUR, data.currency());
        assertEquals(new BigDecimal("149.50"), data.amount());
        assertEquals("credit_card", data.paymentMethod());
    }

    @Test
    void shouldDeserializePaymentFailedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "PAYMENT_FAILED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "PAYMENT_STUCK",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "reason": "Card declined",
                    "errorCode": "CARD_DECLINED"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.PAYMENT_FAILED, event.eventType());
        assertEquals(OrderStatus.PAYMENT_STUCK, event.orderStatus());
        assertInstanceOf(PaymentFailedData.class, event.eventData());
        PaymentFailedData data = (PaymentFailedData) event.eventData();
        assertEquals("Card declined", data.reason());
        assertEquals("CARD_DECLINED", data.errorCode());
    }

    @Test
    void shouldDeserializeShipmentCreatedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "SHIPMENT_CREATED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "SHIPPED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "trackingNumber": "TRACK123456",
                    "carrier": "DHL",
                    "estimatedDelivery": "2024-01-20T10:30:00Z"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.SHIPMENT_CREATED, event.eventType());
        assertEquals(OrderStatus.SHIPPED, event.orderStatus());
        assertInstanceOf(ShipmentCreatedData.class, event.eventData());
        ShipmentCreatedData data = (ShipmentCreatedData) event.eventData();
        assertEquals("TRACK123456", data.trackingNumber());
        assertEquals("DHL", data.carrier());
        assertEquals(OffsetDateTime.parse("2024-01-20T10:30:00Z"), data.estimatedDelivery());
    }

    @Test
    void shouldDeserializeShipmentCompletedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "SHIPMENT_COMPLETED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "DELIVERED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "deliveredAt": "2024-01-20T14:30:00Z",
                    "signedBy": "John Doe"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.SHIPMENT_COMPLETED, event.eventType());
        assertEquals(OrderStatus.DELIVERED, event.orderStatus());
        assertInstanceOf(ShipmentCompletedData.class, event.eventData());
        ShipmentCompletedData data = (ShipmentCompletedData) event.eventData();
        assertEquals(OffsetDateTime.parse("2024-01-20T14:30:00Z"), data.deliveredAt());
        assertEquals("John Doe", data.signedBy());
    }

    @Test
    void shouldDeserializeShipmentFailedEvent() throws Exception {
        String json = """
            {
                "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "eventType": "SHIPMENT_FAILED",
                "orderId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
                "orderStatus": "DELIVERY_FAILED",
                "createdAt": "2024-01-15T10:30:00Z",
                "eventData": {
                    "reason": "Address not found",
                    "errorCode": "ADDRESS_NOT_FOUND"
                }
            }
            """;

        MessageEvent event = deserialize(json);

        assertEquals(EventType.SHIPMENT_FAILED, event.eventType());
        assertEquals(OrderStatus.DELIVERY_FAILED, event.orderStatus());
        assertInstanceOf(ShipmentFailedData.class, event.eventData());
        ShipmentFailedData data = (ShipmentFailedData) event.eventData();
        assertEquals("Address not found", data.reason());
        assertEquals("ADDRESS_NOT_FOUND", data.errorCode());
    }

    private MessageEvent deserialize(String json) throws Exception {
        MessageProperties props = new MessageProperties();
        props.setHeader("__TypeId__", MessageEvent.class.getName());
        Message message = new Message(json.getBytes(), props);
        return (MessageEvent) converter.fromMessage(message, MessageEvent.class);
    }
}