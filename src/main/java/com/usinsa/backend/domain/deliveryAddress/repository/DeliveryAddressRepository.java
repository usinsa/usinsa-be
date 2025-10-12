package com.usinsa.backend.domain.deliveryAddress.repository;

import com.usinsa.backend.domain.deliveryAddress.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
}
