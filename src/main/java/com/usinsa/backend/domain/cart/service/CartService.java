package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.cart.repository.CartRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.Product;
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
            // 생성 후에는 상품 정보와 함께 조회
            Cart updated = cartRepository.findByIdWithProduct(cart.getId())
                    .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
            return toResponseDto(updated);
        }

        Cart cart = Cart.builder()
                .member(member)
                .productOption(productOption)
                .count(request.getCount())
                .build();
        Cart saved = cartRepository.save(cart);

        // 저장 후 상품 정보와 함께 조회
        Cart result = cartRepository.findByIdWithProduct(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
        return toResponseDto(result);
    }

    /**
     * 비회원(세션 기반) 장바구니 생성
     */
    public CartDto.Response createGuestCart(CartDto.GuestCreateReq request, String sessionId) {
        validateSessionId(sessionId);

        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new EntityNotFoundException("상품 옵션이 존재하지 않습니다."));

        // 동일 상품이 세션 장바구니에 있는지 확인
        Optional<Cart> existingCart = cartRepository.findBySessionIdAndProductOption(sessionId, productOption);
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            cart.setCount(cart.getCount() + request.getCount());
            log.info("비회원 장바구니 수량 추가 - SessionId: {}, ProductOptionId: {}, Count: {}",
                    sessionId, productOption.getId(), cart.getCount());

            // 수정 후 상품 정보와 함께 조회
            Cart updated = cartRepository.findByIdWithProduct(cart.getId())
                    .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
            return toResponseDto(updated);
        }

        Cart cart = Cart.builder()
                .sessionId(sessionId)
                .productOption(productOption)
                .count(request.getCount())
                .build();
        Cart saved = cartRepository.save(cart);
        log.info("비회원 장바구니 생성 - SessionId: {}, ProductOptionId: {}", sessionId, productOption.getId());

        // 저장 후 상품 정보와 함께 조회
        Cart result = cartRepository.findByIdWithProduct(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
        return toResponseDto(result);
    }

    @Transactional(readOnly = true)
    public CartDto.Response findById(Long id) {
        Cart cart = cartRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));
        return toResponseDto(cart);
    }

    @Transactional(readOnly = true)
    public List<CartDto.Response> findAll() {
        return cartRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * 회원 ID로 장바구니 조회
     */
    @Transactional(readOnly = true)
    public List<CartDto.Response> findByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        return cartRepository.findByMemberWithProduct(member).stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * 세션 ID로 비회원 장바구니 조회
     */
    @Transactional(readOnly = true)
    public List<CartDto.Response> findBySessionId(String sessionId) {
        validateSessionId(sessionId);
        return cartRepository.findBySessionIdWithProduct(sessionId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    public CartDto.Response update(Long id, CartDto.UpdateReq request) {
        Cart cart = cartRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new EntityNotFoundException("장바구니가 존재하지 않습니다."));

        cart.setCount(request.getCount());
        return toResponseDto(cart);
    }

    public void delete(Long id) {
        if (!cartRepository.existsById(id)) {
            throw new EntityNotFoundException("장바구니가 존재하지 않습니다.");
        }
        cartRepository.deleteById(id);
    }

    /**
     * 비회원 장바구니를 회원 장바구니로 병합 (로그인 시 호출)
     */
    public List<CartDto.Response> mergeGuestCartToMember(String sessionId, Long memberId) {
        validateSessionId(sessionId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        // 비회원 장바구니 항목 조회 (간단 조회)
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
                // 이미 있으면 수량 합산 후 비회원 장바구니 삭제
                Cart memberCart = existingMemberCart.get();
                int originalCount = memberCart.getCount();
                memberCart.setCount(originalCount + guestCart.getCount());
                cartRepository.delete(guestCart);
                log.info("기존 회원 장바구니에 수량 합산 후 비회원 장바구니 삭제 - ProductOptionId: {}, 기존: {}, 추가: {}",
                        productOption.getId(), originalCount, guestCart.getCount());
            } else {
                // 없으면 비회원 장바구니를 회원 장바구니로 변환
                guestCart.setMember(member);
                guestCart.setSessionId(null);
                log.info("비회원 장바구니를 회원 장바구니로 변환 - ProductOptionId: {}", productOption.getId());
            }
        }

        log.info("비회원 장바구니 병합 완료 - MemberId: {}", memberId);

        return findByMemberId(memberId);
    }

    /**
     * 세션 장바구니 전체 삭제 (컨트롤러용)
     */
    public void deleteGuestCart(String sessionId) {
        validateSessionId(sessionId);
        cartRepository.deleteBySessionId(sessionId);
        log.info("비회원 장바구니 삭제 - SessionId: {}", sessionId);
    }

    /**
     * 세션 ID로 비회원 장바구니 삭제 (세션 리스너용)
     * 
     * @param sessionId 세션 ID
     * @return 삭제된 장바구니 항목 수
     */
    public int deleteGuestCartBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("유효하지 않은 세션 ID - 비회원 장바구니 삭제 건너뜀");
            return 0;
        }
        
        List<Cart> guestCarts = cartRepository.findBySessionId(sessionId);
        int count = guestCarts.size();
        
        if (count > 0) {
            cartRepository.deleteBySessionId(sessionId);
            log.info("세션 만료로 인한 비회원 장바구니 삭제 - SessionId: {}, 삭제 항목 수: {}", 
                    sessionId, count);
        }
        
        return count;
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("세션 ID가 필요합니다.");
        }
    }

    private CartDto.Response toResponseDto(Cart cart) {
        ProductOption productOption = cart.getProductOption();
        Product product = productOption.getProduct();

        CartDto.ProductInfo productInfo = CartDto.ProductInfo.builder()
                .productId(product.getId())
                .productName(product.getName())
                .brandName(product.getBrandName())
                .price(product.getPrice())
                .optionName(productOption.getOptionName())
                .stock(productOption.getStock())
                .build();

        return CartDto.Response.builder()
                .id(cart.getId())
                .memberId(cart.getMember() != null ? cart.getMember().getId() : null)
                .sessionId(cart.getSessionId())
                .productOptionId(productOption.getId())
                .count(cart.getCount())
                .guest(cart.isGuestCart())
                .productInfo(productInfo)
                .build();
    }
}
