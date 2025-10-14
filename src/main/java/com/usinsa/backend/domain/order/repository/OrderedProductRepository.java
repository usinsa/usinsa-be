package com.usinsa.backend.domain.order.repository;

import com.usinsa.backend.domain.order.entity.OrderedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderedProductRepository extends JpaRepository<OrderedProduct, Long> {
}