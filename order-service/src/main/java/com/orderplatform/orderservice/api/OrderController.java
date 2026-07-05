package com.orderplatform.orderservice.api;

import com.orderplatform.orderservice.api.request.CreateOrderRequest;
import com.orderplatform.orderservice.api.response.OrderResponse;
import com.orderplatform.orderservice.application.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) String customerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrders(customerId, pageable));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cancelOrder(@PathVariable UUID id) {
        orderService.cancelOrder(id);
    }

    @PostMapping("/{id}/fulfil")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void fulfilOrder(@PathVariable UUID id) {
        orderService.fulfilOrder(id);
    }
}
