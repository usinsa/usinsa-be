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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 회원용 장바구니 생성
     */
    public CartDto.Response create(CartDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new EntityNotFoundException("상품 옵션이 존재하지 않습니다."));

        // 동일 상품이 이미 장바구니에 있는지 확인
        Optional<Cart> existingCart = cartRepository.findByMemberAndProductOption(member, productOption);
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            cart.setCount(cart.getCount() + request.getCount());
            return toResDto(cart);
        }

        Cart cart = Cart.builder()
                .member(member)
                .productOption(productOption)
                .count(request.getCount())
                .build();
        Cart saved = cartRepository.save(cart);
        return toResDto(saved);
    }

    /**
     * 비회원(세션 기반) 장바구니 생성
     */
    public CartDto.Response createGuestCart(CartDto.GuestCreateReq request, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("세션 ID가 필요합니다.");
        }

        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new EntityNotFoundException("상품 옵션이 존재하지 않습니다."));

        // 동일 상품이 세션 장바구니에 있는지 확인
        Optional<Cart> existingCart = cartRepository.findBySessionIdAndProductOption(sessionId, productOption);
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            cart.setCount(cart.getCount() + request.getCount());
            return toResDto(cart);
        }

        Cart cart = Cart.builder()
                .sessionId(sessionId)
                .productOption(productOption)
                .count(request.getCount())
                .build();
        Cart saved = cartRepository.save(cart);
        log.info("비회원 장바구니 생성 - SessionId: {}, ProductOptionId: {}", sessionId, productOption.getId());
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

    /**
     * 회원 ID로 장바구니 조회
     */
    @Transactional(readOnly = true)
    public List<CartDto.Response> findByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        return cartRepository.findByMember(member).stream()
                .map(this::toResDto)
                .toList();
    }

    /**
     * 세션 ID로 비회원 장바구니 조회
     */
    @Transactional(readOnly = true)
    public List<CartDto.Response> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("세션 ID가 필요합니다.");
        }
        return cartRepository.findBySessionId(sessionId).stream()
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

    /**
     * 비회원 장바구니를 회원 장바구니로 병합 (로그인 시 호출)
     * 1. 세션 ID로 비회원 장바구니를 조회
     * 2. 각 항목을 회원 장바구니로 변환
     * 3. 동일 상품이 있으면 수량을 합산
     * 4. 비회원 장바구니 항목 삭제
     */
    public List<CartDto.Response> mergeGuestCartToMember(String sessionId, Long memberId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("세션 ID가 필요합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        // 비회원 장바구니 항목 조회
        List<Cart> guestCarts = cartRepository.findBySessionId(sessionId);

        if (guestCarts.isEmpty()) {
            log.info("병합할 비회원 장바구니 항목이 없습니다. SessionId: {}", sessionId);
            return findByMemberId(memberId);
        }

        log.info("비회원 장바구니 병합 시작 - SessionId: {}, MemberId: {}, 항목 수: {}",
                sessionId, memberId, guestCarts.size());

        for (Cart guestCart : guestCarts) {
            ProductOption productOption = guestCart.getProductOption();

            // 회원 장바구니에 동일 상품이 있는지 확인
            Optional<Cart> existingMemberCart = cartRepository.findByMemberAndProductOption(member, productOption);

            if (existingMemberCart.isPresent()) {
                // 이미 있으면 수량 합산
                Cart memberCart = existingMemberCart.get();
                memberCart.setCount(memberCart.getCount() + guestCart.getCount());
                log.info("기존 회원 장바구니에 수량 합산 - ProductOptionId: {}, 기존: {}, 추가: {}",
                        productOption.getId(), memberCart.getCount() - guestCart.getCount(), guestCart.getCount());
            } else {
                // 없으면 비회원 장바구니를 회원 장바구니로 변환
                guestCart.setMember(member);
                guestCart.setSessionId(null);
                log.info("비회원 장바구니를 회원 장바구니로 변환 - ProductOptionId: {}", productOption.getId());
            }
        }

        // 비회원 장바구니에서 회원으로 변환된 항목은 자동으로 member_id가 설정되므로
        // 남은 sessionId만 있는 항목들을 삭제 (이미 변환된 경우)
        cartRepository.deleteBySessionId(sessionId);

        log.info("비회원 장바구니 병합 완료 - MemberId: {}", memberId);

        return findByMemberId(memberId);
    }

    /**
     * 세션 장바구니 전체 삭제
     */
    public void deleteGuestCart(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("세션 ID가 필요합니다.");
        }
        cartRepository.deleteBySessionId(sessionId);
        log.info("비회원 장바구니 삭제 - SessionId: {}", sessionId);
    }

    private CartDto.Response toResDto(Cart cart) {
        return CartDto.Response.builder()
                .id(cart.getId())
                .memberId(cart.getMember() != null ? cart.getMember().getId() : null)
                .sessionId(cart.getSessionId())
                .productOptionId(cart.getProductOption().getId())
                .count(cart.getCount())
                .guest(cart.isGuestCart())
                .build();
    }
}