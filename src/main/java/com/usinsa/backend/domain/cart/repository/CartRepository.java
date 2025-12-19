package com.usinsa.backend.domain.cart.repository;

import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.product.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // 상품 정보와 함께 조회 (프론트엔드 표시용)
    @Query("SELECT c FROM Cart c " +
            "JOIN FETCH c.productOption po " +
            "JOIN FETCH po.product p " +
            "LEFT JOIN FETCH c.member " +
            "WHERE c.id = :id")
    Optional<Cart> findByIdWithProduct(@Param("id") Long id);

    @Query("SELECT c FROM Cart c " +
            "JOIN FETCH c.productOption po " +
            "JOIN FETCH po.product p " +
            "LEFT JOIN FETCH c.member " +
            "WHERE c.member = :member")
    List<Cart> findByMemberWithProduct(@Param("member") Member member);

    @Query("SELECT c FROM Cart c " +
            "JOIN FETCH c.productOption po " +
            "JOIN FETCH po.product p " +
            "WHERE c.sessionId = :sessionId")
    List<Cart> findBySessionIdWithProduct(@Param("sessionId") String sessionId);

    // 장바구니 생성/수정용 (간단 조회)
    Optional<Cart> findByMemberAndProductOption(Member member, ProductOption productOption);

    Optional<Cart> findBySessionIdAndProductOption(String sessionId, ProductOption productOption);

    List<Cart> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
