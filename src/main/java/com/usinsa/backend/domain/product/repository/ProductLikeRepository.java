package com.usinsa.backend.domain.product.repository;

import com.usinsa.backend.domain.product.entity.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    @Query("SELECT pl FROM ProductLike pl WHERE pl.member.id = :memberId AND pl.product.id = :productId")
    Optional<ProductLike> findByMemberIdAndProductId(@Param("memberId") Long memberId, @Param("productId") Long productId);

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    @Query("SELECT COUNT(pl) FROM ProductLike pl WHERE pl.product.id = :productId")
    int countByProductId(@Param("productId") Long productId);

    @Query("SELECT pl FROM ProductLike pl JOIN FETCH pl.product WHERE pl.member.id = :memberId")
    List<ProductLike> findByMemberId(@Param("memberId") Long memberId);
}
