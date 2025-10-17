package com.usinsa.backend.domain.deliveryAddress.repository;

import com.usinsa.backend.domain.deliveryAddress.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

    @EntityGraph(attributePaths = {"member"})
    Optional<DeliveryAddress> findWithMemberById(Long id);

    @Override
    @EntityGraph(attributePaths = {"member"})
    List<DeliveryAddress> findAll();

}
