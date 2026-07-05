package com.orderplatform.orderservice.repository;

import com.orderplatform.orderservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByIdIn(Collection<UUID> ids);
}
