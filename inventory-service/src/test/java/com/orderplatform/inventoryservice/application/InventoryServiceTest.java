package com.orderplatform.inventoryservice.application;

import com.orderplatform.common.event.OrderCreatedEvent;
import com.orderplatform.common.event.OrderItemEvent;
import com.orderplatform.inventoryservice.api.request.AddStockRequest;
import com.orderplatform.inventoryservice.api.response.InventoryItemResponse;
import com.orderplatform.inventoryservice.domain.InventoryItem;
import com.orderplatform.inventoryservice.domain.InsufficientStockException;
import com.orderplatform.inventoryservice.domain.Reservation;
import com.orderplatform.inventoryservice.domain.ReservationStatus;
import com.orderplatform.inventoryservice.repository.InventoryItemRepository;
import com.orderplatform.inventoryservice.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void getStock_found_returnsResponse() {
        UUID productId = UUID.randomUUID();
        InventoryItem item = buildItem(productId, "Widget A", 100, 0);
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(item));

        InventoryItemResponse response = inventoryService.getStock(productId);

        assertThat(response.quantityAvailable()).isEqualTo(100);
        assertThat(response.productName()).isEqualTo("Widget A");
    }

    @Test
    void getStock_notFound_throws() {
        UUID productId = UUID.randomUUID();
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStock(productId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addStock_newProduct_createsItem() {
        UUID productId = UUID.randomUUID();
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        AddStockRequest request = new AddStockRequest(productId, "New Widget", 50);
        InventoryItemResponse response = inventoryService.addStock(request);

        assertThat(response.quantityAvailable()).isEqualTo(50);
        assertThat(response.productName()).isEqualTo("New Widget");
    }

    @Test
    void addStock_existingProduct_incrementsQuantity() {
        UUID productId = UUID.randomUUID();
        InventoryItem existing = buildItem(productId, "Widget A", 100, 0);
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddStockRequest request = new AddStockRequest(productId, "Widget A", 50);
        InventoryItemResponse response = inventoryService.addStock(request);

        assertThat(response.quantityAvailable()).isEqualTo(150);
    }

    @Test
    void reserveStock_sufficientStock_createsReservation() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryItem item = buildItem(productId, "Widget A", 100, 0);

        when(reservationRepository.existsByOrderIdAndProductId(orderId, productId)).thenReturn(false);
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "cust-001",
                List.of(new OrderItemEvent(productId, 10)), Instant.now());

        inventoryService.reserveStock(event);

        assertThat(item.getQuantityReserved()).isEqualTo(10);
    }

    @Test
    void reserveStock_insufficientStock_throwsException() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryItem item = buildItem(productId, "Widget A", 5, 0);

        when(reservationRepository.existsByOrderIdAndProductId(orderId, productId)).thenReturn(false);
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(item));

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "cust-001",
                List.of(new OrderItemEvent(productId, 10)), Instant.now());

        assertThatThrownBy(() -> inventoryService.reserveStock(event))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void reserveStock_duplicate_skipsIdempotently() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(reservationRepository.existsByOrderIdAndProductId(orderId, productId)).thenReturn(true);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "cust-001",
                List.of(new OrderItemEvent(productId, 10)), Instant.now());

        inventoryService.reserveStock(event);
        // no exception, no save calls
    }

    @Test
    void releaseStock_reservedReservations_decrementsReserved() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryItem item = buildItem(productId, "Widget A", 100, 10);
        Reservation reservation = buildReservation(orderId, productId, 10);

        when(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.releaseStock(orderId);

        assertThat(item.getQuantityReserved()).isEqualTo(0);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void releaseStock_noReservations_noOp() {
        UUID orderId = UUID.randomUUID();
        when(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of());

        inventoryService.releaseStock(orderId);

        verify(inventoryItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void fulfilStock_reservedReservations_decrementsAvailableAndReserved() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryItem item = buildItem(productId, "Widget A", 100, 10);
        Reservation reservation = buildReservation(orderId, productId, 10);

        when(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED))
                .thenReturn(List.of(reservation));
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.fulfilStock(orderId);

        assertThat(item.getQuantityReserved()).isEqualTo(0);
        assertThat(item.getQuantityAvailable()).isEqualTo(90);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
    }

    private InventoryItem buildItem(UUID productId, String name, int available, int reserved) {
        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setProductName(name);
        item.setQuantityAvailable(available);
        item.setQuantityReserved(reserved);
        return item;
    }

    private Reservation buildReservation(UUID orderId, UUID productId, int quantity) {
        Reservation r = new Reservation();
        r.setOrderId(orderId);
        r.setProductId(productId);
        r.setQuantity(quantity);
        r.setStatus(ReservationStatus.RESERVED);
        return r;
    }
}
