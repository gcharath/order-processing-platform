package com.orderplatform.inventoryservice.repository;

import com.orderplatform.inventoryservice.domain.Reservation;
import com.orderplatform.inventoryservice.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByOrderIdAndProductId(UUID orderId, UUID productId);

    List<Reservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
