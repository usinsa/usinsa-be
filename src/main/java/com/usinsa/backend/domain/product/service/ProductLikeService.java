package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.dto.ProductLikeDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductLike;
import com.usinsa.backend.domain.product.repository.ProductLikeRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ProductLikeDto.Response addLike(Long memberId, Long productId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 이미 좋아요를 누른 경우
        if (productLikeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw new IllegalStateException("이미 좋아요를 누른 상품입니다.");
        }

        ProductLike productLike = ProductLike.builder()
                .member(member)
                .product(product)
                .build();

        productLikeRepository.save(productLike);

        int likeCount = productLikeRepository.countByProductId(productId);

        return ProductLikeDto.Response.builder()
                .productId(productId)
                .liked(true)
                .likeCount(likeCount)
                .build();
    }

    @Transactional
    public ProductLikeDto.Response removeLike(Long memberId, Long productId) {
        ProductLike productLike = productLikeRepository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new IllegalArgumentException("좋아요를 누르지 않은 상품입니다."));

        productLikeRepository.delete(productLike);

        int likeCount = productLikeRepository.countByProductId(productId);

        return ProductLikeDto.Response.builder()
                .productId(productId)
                .liked(false)
                .likeCount(likeCount)
                .build();
    }

    public ProductLikeDto.StatusResponse getLikeStatus(Long memberId, Long productId) {
        boolean liked = productLikeRepository.existsByMemberIdAndProductId(memberId, productId);

        return ProductLikeDto.StatusResponse.builder()
                .productId(productId)
                .liked(liked)
                .build();
    }

    public int getLikeCount(Long productId) {
        return productLikeRepository.countByProductId(productId);
    }
}
