package com.orderplatform.orderservice.application;

import com.orderplatform.orderservice.api.request.CreateOrderRequest;
import com.orderplatform.orderservice.api.response.OrderResponse;
import com.orderplatform.orderservice.domain.Order;
import com.orderplatform.orderservice.domain.OrderItem;
import com.orderplatform.orderservice.domain.OrderStatus;
import com.orderplatform.orderservice.domain.Product;
import com.orderplatform.orderservice.repository.OrderRepository;
import com.orderplatform.orderservice.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderEventPublisher orderEventPublisher;

    public Page<OrderResponse> listOrders(String customerId, Pageable pageable) {
        Page<Order> orderPage = customerId != null
                ? orderRepository.findAllByCustomerId(customerId, pageable)
                : orderRepository.findAll(pageable);

        if (!orderPage.isEmpty()) {
            List<UUID> ids = orderPage.getContent().stream().map(Order::getId).toList();
            orderRepository.findAllWithItemsByIdIn(ids);
        }
        log.debug("Listing orders customerId={}", customerId);
        return orderPage.map(OrderResponse::from);
    }

    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        log.debug("Getting order id={}", id);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Set<UUID> productIds = request.items().stream()
                .map(item -> item.productId())
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAllByIdIn(productIds);
        if (products.size() != productIds.size()) {
            throw new EntityNotFoundException("One or more products not found");
        }

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Order order = new Order();
        order.setCustomerId(request.customerId());

        List<OrderItem> items = request.items().stream()
                .map(itemReq -> {
                    Product product = productMap.get(itemReq.productId());
                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setProductId(itemReq.productId());
                    item.setQuantity(itemReq.quantity());
                    item.setUnitPrice(product.getPrice());
                    item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
                    return item;
                })
                .toList();

        order.setItems(new ArrayList<>(items));
        order.setTotalAmount(items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        Order saved = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public void cancelOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        if (!Set.of(OrderStatus.PENDING, OrderStatus.CONFIRMED).contains(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLING);
        orderRepository.save(order);
        orderEventPublisher.publishOrderCancelled(order);
    }

    @Transactional
    public void fulfilOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot fulfil order in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.FULFILLING);
        orderRepository.save(order);
        orderEventPublisher.publishOrderFulfilled(order);
    }
}
