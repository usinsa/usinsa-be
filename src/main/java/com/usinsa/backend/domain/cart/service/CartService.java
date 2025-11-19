package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.cart.repository.CartRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;

    public CartDto.Response create(CartDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new EntityNotFoundException("상품 옵션이 존재하지 않습니다."));

        Cart cart = toEntity(request, member, productOption);
        Cart saved = cartRepository.save(cart);
        return toResDto(saved);
    }

    @Transactional(readOnly = true)
    public CartDto.Response findById(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
        return toResDto(cart);
    }

    @Transactional(readOnly = true)
    public List<CartDto.Response> findAll() {
        return cartRepository.findAll().stream()
                .map(this::toResDto)
                .toList();
    }

    public CartDto.Response update(Long id, CartDto.UpdateReq request) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));

        cart.setCount(request.getCount());
        return toResDto(cart);
    }

    public void delete(Long id) {
        if (!cartRepository.existsById(id)) {
            throw new EntityNotFoundException("장바구니가 존재하지 않습니다.");
        }
        cartRepository.deleteById(id);
    }

    private Cart toEntity(CartDto.CreateReq request, Member member, ProductOption productOption) {
        return Cart.builder()
                .member(member)
                .productOption(productOption)
                .count(request.getCount())
                .build();
    }

    private CartDto.Response toResDto(Cart cart) {
        return CartDto.Response.builder()
                .id(cart.getId())
                .memberId(cart.getMember().getId())
                .productOptionId(cart.getProductOption().getId())
                .count(cart.getCount())
                .build();
    }
}