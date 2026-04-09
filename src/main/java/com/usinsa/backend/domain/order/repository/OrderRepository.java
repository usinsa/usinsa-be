package com.usinsa.backend.domain.order.repository;

import com.usinsa.backend.domain.delivery.entity.Delivery;
import com.usinsa.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"member", "delivery"})
    Optional<Order> findWithMemberAndDeliveryById(Long id);

    @Override
    @EntityGraph(attributePaths = {"member"}) // N+1 문제 방지: Order를 즉시 로딩
    List<Order> findAll();
}