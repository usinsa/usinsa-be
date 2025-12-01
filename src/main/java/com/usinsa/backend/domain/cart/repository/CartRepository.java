package com.usinsa.backend.domain.cart.repository;

import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @Override
    @EntityGraph(attributePaths = {"productOption", "member"})
    List<Cart> findAll();

    @EntityGraph(attributePaths = {"productOption", "member"})
    List<Cart> findBySessionId(String sessionId);

    @EntityGraph(attributePaths = {"productOption", "member"})
    List<Cart> findByMember(Member member);

    Optional<Cart> findBySessionIdAndProductOption(String sessionId, ProductOption productOption);

    Optional<Cart> findByMemberAndProductOption(Member member, ProductOption productOption);

    void deleteBySessionId(String sessionId);
}