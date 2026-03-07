package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.cart.repository.CartRepository;
import com.usinsa.backend.domain.cart.repository.GuestCartRedisRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;
    private final GuestCartRedisRepository guestCartRedis;

    // ── 회원 장바구니 ──────────────────────────────────────────────────

    public CartDto.Response create(CartDto.CreateReq request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        Optional<Cart> existing = cartRepository.findByMemberAndProductOption(member, productOption);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            cart.setCount(cart.getCount() + request.getCount());
            return toResponseDto(cartRepository.findByIdWithProduct(cart.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND)));
        }

        Cart saved = cartRepository.save(Cart.builder()
                .member(member).productOption(productOption).count(request.getCount()).build());
        return toResponseDto(cartRepository.findByIdWithProduct(saved.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND)));
    }

    // ── 비회원 장바구니 (Redis + DB) ──────────────────────────────────

    /**
     * 비회원 장바구니 생성
     * DB에 Cart 행을 저장하고, 해당 Cart.id를 Redis(guest:cart:{guestId})에 등록한다.
     */
    public CartDto.Response createGuestCart(CartDto.GuestCreateReq request, String guestId) {
        validateGuestId(guestId);
        ProductOption productOption = productOptionRepository.findById(request.getProductOptionId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 같은 상품이 이미 있으면 수량 합산
        Optional<Cart> existing = cartRepository.findBySessionIdAndProductOption(guestId, productOption);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            cart.setCount(cart.getCount() + request.getCount());
            guestCartRedis.refreshTtl(guestId);
            return toResponseDto(cartRepository.findByIdWithProduct(cart.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND)));
        }

        Cart saved = cartRepository.save(Cart.builder()
                .sessionId(guestId).productOption(productOption).count(request.getCount()).build());
        guestCartRedis.addCartId(guestId, saved.getId());
        log.info("비회원 장바구니 생성 - guestId={}, cartId={}", guestId, saved.getId());

        return toResponseDto(cartRepository.findByIdWithProduct(saved.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND)));
    }

    /** 비회원 장바구니 목록 조회 — Redis에서 ID 목록 → DB 조회 */
    @Transactional(readOnly = true)
    public List<CartDto.Response> findByGuestId(String guestId) {
        validateGuestId(guestId);

        Set<String> cartIdStrs = guestCartRedis.getCartIds(guestId);
        if (cartIdStrs.isEmpty()) return List.of();

        List<Long> cartIds = cartIdStrs.stream().map(Long::parseLong).toList();
        return cartRepository.findByIdsWithProduct(cartIds).stream()
                .map(this::toResponseDto).toList();
    }

    /** 비회원 장바구니 전체 삭제 */
    public void deleteGuestCart(String guestId) {
        validateGuestId(guestId);
        cartRepository.deleteBySessionId(guestId);
        guestCartRedis.deleteAll(guestId);
        log.info("비회원 장바구니 삭제 - guestId={}", guestId);
    }

    /** 비회원 → 회원 장바구니 병합 */
    public List<CartDto.Response> mergeGuestCartToMember(String guestId, Long memberId) {
        validateGuestId(guestId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Cart> guestCarts = cartRepository.findBySessionId(guestId);
        if (guestCarts.isEmpty()) return findByMemberId(memberId);

        for (Cart guestCart : guestCarts) {
            ProductOption productOption = guestCart.getProductOption();
            Optional<Cart> memberCart = cartRepository.findByMemberAndProductOption(member, productOption);
            if (memberCart.isPresent()) {
                memberCart.get().setCount(memberCart.get().getCount() + guestCart.getCount());
                cartRepository.delete(guestCart);
            } else {
                guestCart.setMember(member);
                guestCart.setSessionId(null);
            }
        }

        guestCartRedis.deleteAll(guestId);
        log.info("비회원 장바구니 병합 완료 - guestId={}, memberId={}", guestId, memberId);
        return findByMemberId(memberId);
    }

    // ── 공통 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CartDto.Response findById(Long id) {
        return toResponseDto(cartRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<CartDto.Response> findAll() {
        return cartRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CartDto.Response> findByMemberId(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return cartRepository.findByMemberWithProduct(member).stream()
                .map(this::toResponseDto).toList();
    }

    public CartDto.Response update(Long id, CartDto.UpdateReq request) {
        Cart cart = cartRepository.findByIdWithProduct(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));
        cart.setCount(request.getCount());
        return toResponseDto(cart);
    }

    public void delete(Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));
        // 비회원 장바구니라면 Redis에서도 제거
        if (cart.getSessionId() != null) {
            guestCartRedis.removeCartId(cart.getSessionId(), id);
        }
        cartRepository.deleteById(id);
    }

    private void validateGuestId(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new CustomException(ErrorCode.SESSION_ID_REQUIRED);
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
