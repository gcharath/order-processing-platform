package com.orderplatform.inventoryservice.messaging;

import com.orderplatform.common.event.OrderCancelledEvent;
import com.orderplatform.common.event.StockReleasedEvent;
import com.orderplatform.inventoryservice.application.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledConsumer {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    public void handle(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent for order {}", event.orderId());
        inventoryService.releaseStock(event.orderId());
        kafkaTemplate.send("inventory.stock-released", event.orderId().toString(),
                new StockReleasedEvent(event.orderId()));
        log.info("Stock released for order {}", event.orderId());
    }
}
