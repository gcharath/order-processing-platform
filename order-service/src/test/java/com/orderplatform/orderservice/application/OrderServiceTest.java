package com.orderplatform.orderservice.application;

import com.orderplatform.orderservice.api.request.CreateOrderRequest;
import com.orderplatform.orderservice.api.request.OrderItemRequest;
import com.orderplatform.orderservice.api.response.OrderResponse;
import com.orderplatform.orderservice.domain.Order;
import com.orderplatform.orderservice.domain.OrderStatus;
import com.orderplatform.orderservice.domain.Product;
import com.orderplatform.orderservice.repository.OrderRepository;
import com.orderplatform.orderservice.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_success() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(19.99));

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CreateOrderRequest request = new CreateOrderRequest(
                "cust-001", List.of(new OrderItemRequest(productId, 2)));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.customerId()).isEqualTo("cust-001");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(39.98));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void createOrder_productNotFound_throws() {
        when(productRepository.findAllByIdIn(any())).thenReturn(List.of());

        CreateOrderRequest request = new CreateOrderRequest(
                "cust-001", List.of(new OrderItemRequest(UUID.randomUUID(), 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getOrder_notFound_throws() {
        when(orderRepository.findWithItemsById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listOrders_noFilter_returnsAll() {
        Order order = buildOrder("cust-001");
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findAllWithItemsByIdIn(any())).thenReturn(List.of(order));

        Page<OrderResponse> result = orderService.listOrders(null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).customerId()).isEqualTo("cust-001");
    }

    @Test
    void listOrders_withCustomerFilter() {
        Order order = buildOrder("cust-002");
        when(orderRepository.findAllByCustomerId(eq("cust-002"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findAllWithItemsByIdIn(any())).thenReturn(List.of(order));

        Page<OrderResponse> result = orderService.listOrders("cust-002", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllByCustomerId(eq("cust-002"), any(Pageable.class));
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void cancelOrder_confirmedOrder_setsStatusCancelling() {
        Order order = buildOrder("cust-001");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLING);
        verify(orderEventPublisher).publishOrderCancelled(order);
    }

    @Test
    void cancelOrder_pendingOrder_setsStatusCancelling() {
        Order order = buildOrder("cust-001");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLING);
    }

    @Test
    void cancelOrder_failedOrder_throwsIllegalState() {
        Order order = buildOrder("cust-001");
        order.setStatus(OrderStatus.FAILED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fulfilOrder_confirmedOrder_setsStatusFulfilling() {
        Order order = buildOrder("cust-001");
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.fulfilOrder(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLING);
        verify(orderEventPublisher).publishOrderFulfilled(order);
    }

    @Test
    void fulfilOrder_pendingOrder_throwsIllegalState() {
        Order order = buildOrder("cust-001");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.fulfilOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    private Order buildOrder(String customerId) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.TEN);
        return order;
    }
}
