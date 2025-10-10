package com.usinsa.backend.domain.delivery.repository;

import com.usinsa.backend.domain.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}