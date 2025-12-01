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
@DisplayName("CartService 세션 장바구니 테스트")
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

        Product testProduct = Product.builder()
                .id(1L)
                .name("테스트상품")
                .build();

        testProductOption1 = ProductOption.builder()
                .id(1L)
                .optionName("M 사이즈")
                .stock(100)
                .product(testProduct)
                .build();

        testProductOption2 = ProductOption.builder()
                .id(2L)
                .optionName("L 사이즈")
                .stock(50)
                .product(testProduct)
                .build();
    }

    @Nested
    @DisplayName("비회원 장바구니 생성 테스트")
    class CreateGuestCartTest {

        @Test
        @DisplayName("비회원이 정상적으로 장바구니에 상품을 추가한다")
        void createGuestCart_Success() {
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

            // when
            CartDto.Response result = cartService.createGuestCart(request, testSessionId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSessionId()).isEqualTo(testSessionId);
            assertThat(result.getMemberId()).isNull();
            assertThat(result.getProductOptionId()).isEqualTo(1L);
            assertThat(result.getCount()).isEqualTo(2);
            assertThat(result.isGuest()).isTrue();
        }

        @Test
        @DisplayName("동일 상품이 세션 장바구니에 이미 있으면 수량을 합산한다")
        void createGuestCart_DuplicateProduct_IncrementCount() {
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

            // when
            CartDto.Response result = cartService.createGuestCart(request, testSessionId);

            // then
            assertThat(result.getCount()).isEqualTo(5); // 2 + 3
            assertThat(existingCart.getCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("세션 ID가 null이면 예외가 발생한다")
        void createGuestCart_NullSessionId_ThrowsException() {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(2)
                    .build();

            // when & then
            assertThatThrownBy(() -> cartService.createGuestCart(request, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }

        @Test
        @DisplayName("상품 옵션이 없으면 예외가 발생한다")
        void createGuestCart_ProductOptionNotFound_ThrowsException() {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(999L)
                    .count(2)
                    .build();

            given(productOptionRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.createGuestCart(request, testSessionId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("상품 옵션이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("세션 장바구니 조회 테스트")
    class FindBySessionIdTest {

        @Test
        @DisplayName("세션 ID로 장바구니를 조회한다")
        void findBySessionId_Success() {
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

            given(cartRepository.findBySessionId(testSessionId))
                    .willReturn(Arrays.asList(cart1, cart2));

            // when
            List<CartDto.Response> results = cartService.findBySessionId(testSessionId);

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getSessionId()).isEqualTo(testSessionId);
            assertThat(results.get(0).isGuest()).isTrue();
            assertThat(results.get(1).getSessionId()).isEqualTo(testSessionId);
            assertThat(results.get(1).isGuest()).isTrue();
        }

        @Test
        @DisplayName("세션 ID가 null이면 예외가 발생한다")
        void findBySessionId_NullSessionId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> cartService.findBySessionId(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 병합 테스트")
    class MergeGuestCartTest {

        @Test
        @DisplayName("비회원 장바구니를 회원 장바구니로 병합한다")
        void mergeGuestCartToMember_Success() {
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
            given(cartRepository.findByMember(testMember))
                    .willReturn(Collections.singletonList(guestCart));
            doNothing().when(cartRepository).deleteBySessionId(testSessionId);

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(results).isNotEmpty();
            assertThat(guestCart.getMember()).isEqualTo(testMember);
            assertThat(guestCart.getSessionId()).isNull();
            verify(cartRepository).deleteBySessionId(testSessionId);
        }

        @Test
        @DisplayName("동일 상품이 회원 장바구니에 있으면 수량을 합산한다")
        void mergeGuestCartToMember_DuplicateProduct_IncrementCount() {
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
            given(cartRepository.findByMember(testMember))
                    .willReturn(Collections.singletonList(memberCart));
            doNothing().when(cartRepository).deleteBySessionId(testSessionId);

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(results).isNotEmpty();
            assertThat(memberCart.getCount()).isEqualTo(5); // 2 + 3
            verify(cartRepository).deleteBySessionId(testSessionId);
        }

        @Test
        @DisplayName("비회원 장바구니가 비어있으면 회원 장바구니만 반환한다")
        void mergeGuestCartToMember_EmptyGuestCart_ReturnMemberCart() {
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
            given(cartRepository.findByMember(testMember))
                    .willReturn(Collections.singletonList(memberCart));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(testSessionId, 1L);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외가 발생한다")
        void mergeGuestCartToMember_MemberNotFound_ThrowsException() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.mergeGuestCartToMember(testSessionId, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("세션 ID가 null이면 예외가 발생한다")
        void mergeGuestCartToMember_NullSessionId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> cartService.mergeGuestCartToMember(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("세션 장바구니 삭제 테스트")
    class DeleteGuestCartTest {

        @Test
        @DisplayName("세션 장바구니를 전체 삭제한다")
        void deleteGuestCart_Success() {
            // given
            doNothing().when(cartRepository).deleteBySessionId(testSessionId);

            // when
            cartService.deleteGuestCart(testSessionId);

            // then
            verify(cartRepository).deleteBySessionId(testSessionId);
        }

        @Test
        @DisplayName("세션 ID가 null이면 예외가 발생한다")
        void deleteGuestCart_NullSessionId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> cartService.deleteGuestCart(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }
    }
}
