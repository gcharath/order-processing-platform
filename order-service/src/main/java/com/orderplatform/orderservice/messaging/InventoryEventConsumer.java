package com.orderplatform.orderservice.messaging;

import com.orderplatform.common.event.InventoryReservationFailedEvent;
import com.orderplatform.common.event.InventoryReservedEvent;
import com.orderplatform.common.event.StockFulfilledEvent;
import com.orderplatform.common.event.StockReleasedEvent;
import com.orderplatform.orderservice.domain.OrderStatus;
import com.orderplatform.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    @Transactional
    public void onReserved(InventoryReservedEvent event) {
        log.info("Inventory reserved for order {}, updating status to CONFIRMED", event.orderId());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = "inventory.reservation-failed", groupId = "order-service")
    @Transactional
    public void onFailed(InventoryReservationFailedEvent event) {
        log.warn("Inventory reservation failed for order {}: {}", event.orderId(), event.reason());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = "inventory.stock-released", groupId = "order-service")
    @Transactional
    public void onStockReleased(StockReleasedEvent event) {
        log.info("Stock released for order {}, updating status to CANCELLED", event.orderId());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = "inventory.stock-fulfilled", groupId = "order-service")
    @Transactional
    public void onStockFulfilled(StockFulfilledEvent event) {
        log.info("Stock fulfilled for order {}, updating status to FULFILLED", event.orderId());
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.FULFILLED);
            orderRepository.save(order);
        });
    }
}
