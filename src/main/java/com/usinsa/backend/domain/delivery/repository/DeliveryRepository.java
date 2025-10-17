package com.usinsa.backend.domain.delivery.repository;

import com.usinsa.backend.domain.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    @EntityGraph(attributePaths = {"order"})
    Optional<Delivery> findWithOrderById(Long id);

    @Override
    @EntityGraph(attributePaths = {"order"}) // N+1 문제 방지: Order를 즉시 로딩
    List<Delivery> findAll();
}