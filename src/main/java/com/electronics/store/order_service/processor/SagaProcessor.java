package com.electronics.store.order_service.processor;

import com.electronics.store.order_service.persistence.service.OrderService;
import com.electronics.store.order_service.rabbit.message.InventoryFailedData;
import com.electronics.store.order_service.rabbit.message.InventoryReservedData;
import com.electronics.store.order_service.rabbit.message.MessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaProcessor {

    private final OrderService orderService;

    public void process(MessageEvent event) {
        switch (event.eventType()) {
            case INVENTORY_RESERVED -> orderService.updateReserved(event.orderId(), (InventoryReservedData) event.eventData());
            case INVENTORY_FAILED -> orderService.updateReservationFailed(event.orderId(), (InventoryFailedData) event.eventData());
        }
    }


}
