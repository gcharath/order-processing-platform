package com.orderplatform.orderservice.application;

import com.orderplatform.common.event.OrderCancelledEvent;
import com.orderplatform.common.event.OrderCreatedEvent;
import com.orderplatform.common.event.OrderFulfilledEvent;
import com.orderplatform.common.event.OrderItemEvent;
import com.orderplatform.orderservice.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        List<OrderItemEvent> items = order.getItems().stream()
                .map(i -> new OrderItemEvent(i.getProductId(), i.getQuantity()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(), order.getCustomerId(), items, order.getCreatedAt());

        kafkaTemplate.send("order.created", order.getId().toString(), event);
        log.info("Published OrderCreatedEvent for order {}", order.getId());
    }

    public void publishOrderCancelled(Order order) {
        kafkaTemplate.send("order.cancelled", order.getId().toString(),
                new OrderCancelledEvent(order.getId()));
        log.info("Published OrderCancelledEvent for order {}", order.getId());
    }

    public void publishOrderFulfilled(Order order) {
        kafkaTemplate.send("order.fulfilled", order.getId().toString(),
                new OrderFulfilledEvent(order.getId()));
        log.info("Published OrderFulfilledEvent for order {}", order.getId());
    }
}
