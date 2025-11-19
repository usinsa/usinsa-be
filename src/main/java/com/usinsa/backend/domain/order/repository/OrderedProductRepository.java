package com.usinsa.backend.domain.order.repository;

import com.usinsa.backend.domain.order.entity.OrderedProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderedProductRepository extends JpaRepository<OrderedProduct, Long> {

    @EntityGraph(attributePaths = {"order", "productOption"})
    Optional<OrderedProduct> findWithOrderAndProductOptionById(Long id);

    @Override
    @EntityGraph(attributePaths = {"order", "productOption"})
    List<OrderedProduct> findAll();
}