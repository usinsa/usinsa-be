package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.entity.Cart;
import com.usinsa.backend.domain.cart.repository.CartRepository;
import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 세션 기반 테스트")
class CartServiceSessionTest {

    @InjectMocks
    private CartService cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductOptionRepository productOptionRepository;

    private Member testMember;
    private Product testProduct;
    private ProductOption testProductOption1;
    private ProductOption testProductOption2;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = "TEST-SESSION-ID-12345";

        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .name("테스트유저")
                .build();

        Category testCategory = Category.builder()
                .id(1L)
                .name("상의")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("오버핏 티셔츠")
                .brandName("무신사 스탠다드")
                .price(29900L)
                .category(testCategory)
                .build();

        testProductOption1 = ProductOption.builder()
                .id(1L)
                .optionName("Black / L")
                .stock(50)
                .product(testProduct)
                .build();

        testProductOption2 = ProductOption.builder()
                .id(2L)
                .optionName("White / M")
                .stock(30)
                .product(testProduct)
                .build();
    }

    @Nested
    @DisplayName("비회원 장바구니 생성")
    class CreateGuestCart {

        @Test
        @DisplayName("비회원이 장바구니에 상품 추가")
        void createSuccess() {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(2)
                    .build();

            Cart guestCart = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption1));
            given(cartRepository.findBySessionIdAndProductOption(testSessionId, testProductOption1))
                    .willReturn(Optional.empty());
            given(cartRepository.save(any(Cart.class))).willReturn(guestCart);
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(guestCart));

            // when
            CartDto.Response result = cartService.createGuestCart(request, testSessionId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isEqualTo(testSessionId);
            assertThat(result.getMemberId()).isNull();
            assertThat(result.isGuest()).isTrue();
            assertThat(result.getProductInfo()).isNotNull();
            assertThat(result.getProductInfo().getProductName()).isEqualTo("오버핏 티셔츠");
        }

        @Test
        @DisplayName("동일 상품이 있으면 수량 합산")
        void incrementCountIfExists() {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(3)
                    .build();

            Cart existingCart = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption1));
            given(cartRepository.findBySessionIdAndProductOption(testSessionId, testProductOption1))
                    .willReturn(Optional.of(existingCart));
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(existingCart));

            // when
            CartDto.Response result = cartService.createGuestCart(request, testSessionId);

            // then
            assertThat(result.getCount()).isEqualTo(5); // 2 + 3
            assertThat(existingCart.getCount()).isEqualTo(5);
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("세션 ID가 없으면 예외 발생")
        void failWithoutSessionId() {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(2)
                    .build();

            // when & then
            assertThatThrownBy(() -> cartService.createGuestCart(request, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");

            assertThatThrownBy(() -> cartService.createGuestCart(request, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("세션 장바구니 조회")
    class FindBySessionId {

        @Test
        @DisplayName("세션 ID로 장바구니 조회 (상품 정보 포함)")
        void findSuccess() {
            // given
            Cart cart1 = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            Cart cart2 = Cart.builder()
                    .id(2L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption2)
                    .count(3)
                    .build();

            given(cartRepository.findBySessionIdWithProduct(testSessionId))
                    .willReturn(Arrays.asList(cart1, cart2));

            // when
            List<CartDto.Response> results = cartService.findBySessionId(testSessionId);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> r.getSessionId().equals(testSessionId));
            assertThat(results).allMatch(CartDto.Response::isGuest);
            assertThat(results).allMatch(r -> r.getProductInfo() != null);
        }

        @Test
        @DisplayName("세션 ID가 없으면 예외 발생")
        void failWithoutSessionId() {
            // when & then
            assertThatThrownBy(() -> cartService.findBySessionId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 병합")
    class MergeGuestCart {

        @Test
        @DisplayName("비회원 장바구니를 회원 장바구니로 병합")
        void mergeSuccess() {
            // given
            Cart guestCart = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(testSessionId))
                    .willReturn(Collections.singletonList(guestCart));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption1))
                    .willReturn(Optional.empty());
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Collections.singletonList(guestCart));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(results).isNotEmpty();
            assertThat(guestCart.getMember()).isEqualTo(testMember);
            assertThat(guestCart.getSessionId()).isNull();
        }

        @Test
        @DisplayName("동일 상품이 있으면 수량 합산 후 비회원 장바구니 삭제")
        void mergeAndIncrementCount() {
            // given
            Cart guestCart = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(3)
                    .build();

            Cart memberCart = Cart.builder()
                    .id(2L)
                    .member(testMember)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(testSessionId))
                    .willReturn(Collections.singletonList(guestCart));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption1))
                    .willReturn(Optional.of(memberCart));
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Collections.singletonList(memberCart));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(memberCart.getCount()).isEqualTo(5); // 2 + 3
            verify(cartRepository).delete(guestCart);
        }

        @Test
        @DisplayName("여러 상품 병합")
        void mergeMultipleProducts() {
            // given
            Cart guestCart1 = Cart.builder()
                    .id(1L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            Cart guestCart2 = Cart.builder()
                    .id(2L)
                    .sessionId(testSessionId)
                    .productOption(testProductOption2)
                    .count(3)
                    .build();

            Cart memberCart = Cart.builder()
                    .id(3L)
                    .member(testMember)
                    .productOption(testProductOption1)
                    .count(1)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(testSessionId))
                    .willReturn(Arrays.asList(guestCart1, guestCart2));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption1))
                    .willReturn(Optional.of(memberCart));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption2))
                    .willReturn(Optional.empty());
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Arrays.asList(memberCart, guestCart2));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(memberCart.getCount()).isEqualTo(3); // 1 + 2
            assertThat(guestCart2.getMember()).isEqualTo(testMember);
            assertThat(guestCart2.getSessionId()).isNull();
            verify(cartRepository).delete(guestCart1);
        }

        @Test
        @DisplayName("비회원 장바구니가 비어있으면 회원 장바구니만 반환")
        void mergeEmptyGuestCart() {
            // given
            Cart memberCart = Cart.builder()
                    .id(1L)
                    .member(testMember)
                    .productOption(testProductOption1)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(testSessionId))
                    .willReturn(Collections.emptyList());
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Collections.singletonList(memberCart));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMemberId()).isEqualTo(1L);
            verify(cartRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 병합 시 예외 발생")
        void failWithInvalidMember() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.mergeGuestCartToMember(testSessionId, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("세션 장바구니 삭제")
    class DeleteGuestCart {

        @Test
        @DisplayName("세션 장바구니 전체 삭제")
        void deleteSuccess() {
            // given
            doNothing().when(cartRepository).deleteBySessionId(testSessionId);

            // when
            cartService.deleteGuestCart(testSessionId);

            // then
            verify(cartRepository).deleteBySessionId(testSessionId);
        }

        @Test
        @DisplayName("세션 ID가 없으면 예외 발생")
        void failWithoutSessionId() {
            // when & then
            assertThatThrownBy(() -> cartService.deleteGuestCart(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }
}
