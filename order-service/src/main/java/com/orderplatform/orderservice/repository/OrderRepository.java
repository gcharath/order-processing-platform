package com.orderplatform.orderservice.repository;

import com.orderplatform.orderservice.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByCustomerId(String customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findWithItemsById(UUID id);

    // Used to batch-load items for a page of orders (avoids N+1 with @BatchSize)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<UUID> ids);
}
