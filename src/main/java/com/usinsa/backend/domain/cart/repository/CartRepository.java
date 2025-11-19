package com.usinsa.backend.domain.cart.repository;

import com.usinsa.backend.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Override
    @EntityGraph(attributePaths = {"productOption", "member"})
    List<Cart> findAll();
}