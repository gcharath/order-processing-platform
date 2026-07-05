package com.orderplatform.inventoryservice.application;

import com.orderplatform.common.event.OrderCreatedEvent;
import com.orderplatform.inventoryservice.api.request.AddStockRequest;
import com.orderplatform.inventoryservice.api.response.InventoryItemResponse;
import com.orderplatform.inventoryservice.domain.InventoryItem;
import com.orderplatform.inventoryservice.domain.InsufficientStockException;
import com.orderplatform.inventoryservice.domain.Reservation;
import com.orderplatform.inventoryservice.domain.ReservationStatus;
import com.orderplatform.inventoryservice.repository.InventoryItemRepository;
import com.orderplatform.inventoryservice.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationRepository reservationRepository;

    public InventoryItemResponse getStock(UUID productId) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("No inventory found for product: " + productId));
        log.debug("Getting stock productId={}", productId);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse addStock(AddStockRequest request) {
        InventoryItem item = inventoryItemRepository.findByProductId(request.productId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProductId(request.productId());
                    newItem.setProductName(request.productName());
                    newItem.setQuantityAvailable(0);
                    newItem.setQuantityReserved(0);
                    return newItem;
                });

        item.setQuantityAvailable(item.getQuantityAvailable() + request.quantity());
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public void reserveStock(OrderCreatedEvent event) {
        for (var item : event.items()) {
            if (reservationRepository.existsByOrderIdAndProductId(event.orderId(), item.productId())) {
                continue; // idempotency guard
            }

            InventoryItem inv = inventoryItemRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No inventory found for product: " + item.productId()));

            int available = inv.getQuantityAvailable() - inv.getQuantityReserved();
            if (available < item.quantity()) {
                throw new InsufficientStockException(item.productId(), item.quantity(), available);
            }

            inv.setQuantityReserved(inv.getQuantityReserved() + item.quantity());
            inventoryItemRepository.save(inv);

            Reservation reservation = new Reservation();
            reservation.setOrderId(event.orderId());
            reservation.setProductId(item.productId());
            reservation.setQuantity(item.quantity());
            reservation.setStatus(ReservationStatus.RESERVED);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void releaseStock(UUID orderId) {
        List<Reservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (Reservation r : reservations) {
            InventoryItem inv = inventoryItemRepository.findByProductId(r.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No inventory found for product: " + r.getProductId()));
            inv.setQuantityReserved(inv.getQuantityReserved() - r.getQuantity());
            inventoryItemRepository.save(inv);
            r.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(r);
        }
    }

    @Transactional
    public void fulfilStock(UUID orderId) {
        List<Reservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        for (Reservation r : reservations) {
            InventoryItem inv = inventoryItemRepository.findByProductId(r.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No inventory found for product: " + r.getProductId()));
            inv.setQuantityReserved(inv.getQuantityReserved() - r.getQuantity());
            inv.setQuantityAvailable(inv.getQuantityAvailable() - r.getQuantity());
            inventoryItemRepository.save(inv);
            r.setStatus(ReservationStatus.FULFILLED);
            reservationRepository.save(r);
        }
    }
}
