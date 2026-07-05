package com.orderplatform.inventoryservice.messaging;

import com.orderplatform.common.event.InventoryReservationFailedEvent;
import com.orderplatform.common.event.InventoryReservedEvent;
import com.orderplatform.common.event.OrderCreatedEvent;
import com.orderplatform.inventoryservice.application.InventoryService;
import com.orderplatform.inventoryservice.domain.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    public void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order {}", event.orderId());
        try {
            inventoryService.reserveStock(event);
            kafkaTemplate.send("inventory.reserved", event.orderId().toString(),
                    new InventoryReservedEvent(event.orderId()));
            log.info("Stock reserved successfully for order {}", event.orderId());
        } catch (InsufficientStockException e) {
            log.warn("Insufficient stock for order {}: {}", event.orderId(), e.getMessage());
            kafkaTemplate.send("inventory.reservation-failed", event.orderId().toString(),
                    new InventoryReservationFailedEvent(event.orderId(), e.getMessage()));
        }
    }
}
