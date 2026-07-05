package com.orderplatform.inventoryservice.messaging;

import com.orderplatform.common.event.OrderFulfilledEvent;
import com.orderplatform.common.event.StockFulfilledEvent;
import com.orderplatform.inventoryservice.application.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFulfilledConsumer {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.fulfilled", groupId = "inventory-service")
    public void handle(OrderFulfilledEvent event) {
        log.info("Received OrderFulfilledEvent for order {}", event.orderId());
        inventoryService.fulfilStock(event.orderId());
        kafkaTemplate.send("inventory.stock-fulfilled", event.orderId().toString(),
                new StockFulfilledEvent(event.orderId()));
        log.info("Stock fulfilled for order {}", event.orderId());
    }
}
