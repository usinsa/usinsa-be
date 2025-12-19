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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

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
    private ProductOption testProductOption;
    private Cart testCart;

    @BeforeEach
    void setUp() {
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

        testProductOption = ProductOption.builder()
                .id(1L)
                .optionName("Black / L")
                .stock(50)
                .product(testProduct)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .member(testMember)
                .productOption(testProductOption)
                .count(2)
                .build();
    }

    @Nested
    @DisplayName("회원 장바구니 생성")
    class CreateMemberCart {

        @Test
        @DisplayName("새 상품을 장바구니에 추가")
        void createNewCart() {
            // given
            CartDto.CreateReq request = CartDto.CreateReq.builder()
                    .memberId(1L)
                    .productOptionId(1L)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption))
                    .willReturn(Optional.empty());
            given(cartRepository.save(any(Cart.class))).willReturn(testCart);
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(testCart));

            // when
            CartDto.Response result = cartService.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo(1L);
            assertThat(result.getProductOptionId()).isEqualTo(1L);
            assertThat(result.getCount()).isEqualTo(2);
            assertThat(result.isGuest()).isFalse();
            assertThat(result.getProductInfo()).isNotNull();
            assertThat(result.getProductInfo().getProductName()).isEqualTo("오버핏 티셔츠");
            assertThat(result.getProductInfo().getBrandName()).isEqualTo("무신사 스탠다드");
            assertThat(result.getProductInfo().getPrice()).isEqualTo(29900L);
            assertThat(result.getProductInfo().getOptionName()).isEqualTo("Black / L");
        }

        @Test
        @DisplayName("이미 있는 상품은 수량만 증가")
        void addToExistingCart() {
            // given
            CartDto.CreateReq request = CartDto.CreateReq.builder()
                    .memberId(1L)
                    .productOptionId(1L)
                    .count(3)
                    .build();

            Cart existingCart = Cart.builder()
                    .id(1L)
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption))
                    .willReturn(Optional.of(existingCart));
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(existingCart));

            // when
            CartDto.Response result = cartService.create(request);

            // then
            assertThat(result.getCount()).isEqualTo(5); // 2 + 3
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 생성 시 예외 발생")
        void createWithInvalidMember() {
            // given
            CartDto.CreateReq request = CartDto.CreateReq.builder()
                    .memberId(999L)
                    .productOptionId(1L)
                    .count(2)
                    .build();

            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 상품 옵션으로 생성 시 예외 발생")
        void createWithInvalidProductOption() {
            // given
            CartDto.CreateReq request = CartDto.CreateReq.builder()
                    .memberId(1L)
                    .productOptionId(999L)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(productOptionRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.create(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("상품 옵션이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("비회원 장바구니 생성")
    class CreateGuestCart {

        @Test
        @DisplayName("새 상품을 비회원 장바구니에 추가")
        void createNewGuestCart() {
            // given
            String sessionId = "test-session-id";
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(2)
                    .build();

            Cart guestCart = Cart.builder()
                    .id(1L)
                    .sessionId(sessionId)
                    .productOption(testProductOption)
                    .count(2)
                    .build();

            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption));
            given(cartRepository.findBySessionIdAndProductOption(sessionId, testProductOption))
                    .willReturn(Optional.empty());
            given(cartRepository.save(any(Cart.class))).willReturn(guestCart);
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(guestCart));

            // when
            CartDto.Response result = cartService.createGuestCart(request, sessionId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isNull();
            assertThat(result.getSessionId()).isEqualTo(sessionId);
            assertThat(result.isGuest()).isTrue();
            assertThat(result.getProductInfo()).isNotNull();
        }

        @Test
        @DisplayName("이미 있는 상품은 수량만 증가")
        void addToExistingGuestCart() {
            // given
            String sessionId = "test-session-id";
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(1L)
                    .count(3)
                    .build();

            Cart existingCart = Cart.builder()
                    .id(1L)
                    .sessionId(sessionId)
                    .productOption(testProductOption)
                    .count(2)
                    .build();

            given(productOptionRepository.findById(1L)).willReturn(Optional.of(testProductOption));
            given(cartRepository.findBySessionIdAndProductOption(sessionId, testProductOption))
                    .willReturn(Optional.of(existingCart));
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(existingCart));

            // when
            CartDto.Response result = cartService.createGuestCart(request, sessionId);

            // then
            assertThat(result.getCount()).isEqualTo(5);
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("세션 ID가 없으면 예외 발생")
        void createWithoutSessionId() {
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
    @DisplayName("장바구니 조회")
    class FindCart {

        @Test
        @DisplayName("ID로 장바구니 조회 (상품 정보 포함)")
        void findById() {
            // given
            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(testCart));

            // when
            CartDto.Response result = cartService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProductInfo()).isNotNull();
            assertThat(result.getProductInfo().getProductName()).isEqualTo("오버핏 티셔츠");
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 조회 시 예외 발생")
        void findByIdNotFound() {
            // given
            given(cartRepository.findByIdWithProduct(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }

        @Test
        @DisplayName("회원 ID로 장바구니 목록 조회")
        void findByMemberId() {
            // given
            Cart cart2 = Cart.builder()
                    .id(2L)
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(3)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Arrays.asList(testCart, cart2));

            // when
            List<CartDto.Response> results = cartService.findByMemberId(1L);

            // then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> r.getProductInfo() != null);
        }

        @Test
        @DisplayName("세션 ID로 비회원 장바구니 조회")
        void findBySessionId() {
            // given
            String sessionId = "test-session-id";
            Cart guestCart = Cart.builder()
                    .id(1L)
                    .sessionId(sessionId)
                    .productOption(testProductOption)
                    .count(2)
                    .build();

            given(cartRepository.findBySessionIdWithProduct(sessionId))
                    .willReturn(List.of(guestCart));

            // when
            List<CartDto.Response> results = cartService.findBySessionId(sessionId);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSessionId()).isEqualTo(sessionId);
            assertThat(results.get(0).isGuest()).isTrue();
        }
    }

    @Nested
    @DisplayName("장바구니 수정")
    class UpdateCart {

        @Test
        @DisplayName("장바구니 수량 변경")
        void updateCount() {
            // given
            CartDto.UpdateReq request = CartDto.UpdateReq.builder()
                    .count(5)
                    .build();

            given(cartRepository.findByIdWithProduct(1L)).willReturn(Optional.of(testCart));

            // when
            CartDto.Response result = cartService.update(1L, request);

            // then
            assertThat(result.getCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 수정 시 예외 발생")
        void updateNotFound() {
            // given
            CartDto.UpdateReq request = CartDto.UpdateReq.builder().count(5).build();
            given(cartRepository.findByIdWithProduct(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.update(999L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 삭제")
    class DeleteCart {

        @Test
        @DisplayName("장바구니 단건 삭제")
        void delete() {
            // given
            given(cartRepository.existsById(1L)).willReturn(true);
            doNothing().when(cartRepository).deleteById(1L);

            // when
            cartService.delete(1L);

            // then
            verify(cartRepository).deleteById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 삭제 시 예외 발생")
        void deleteNotFound() {
            // given
            given(cartRepository.existsById(999L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> cartService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }

        @Test
        @DisplayName("비회원 장바구니 전체 삭제")
        void deleteGuestCart() {
            // given
            String sessionId = "test-session-id";
            doNothing().when(cartRepository).deleteBySessionId(sessionId);

            // when
            cartService.deleteGuestCart(sessionId);

            // then
            verify(cartRepository).deleteBySessionId(sessionId);
        }
    }

    @Nested
    @DisplayName("비회원 장바구니 병합")
    class MergeGuestCart {

        @Test
        @DisplayName("비회원 장바구니를 회원 장바구니로 병합")
        void mergeSuccess() {
            // given
            String sessionId = "test-session-id";

            ProductOption option2 = ProductOption.builder()
                    .id(2L)
                    .optionName("White / M")
                    .stock(30)
                    .product(testProduct)
                    .build();

            Cart guestCart1 = Cart.builder()
                    .id(2L)
                    .sessionId(sessionId)
                    .productOption(testProductOption)
                    .count(3)
                    .build();

            Cart guestCart2 = Cart.builder()
                    .id(3L)
                    .sessionId(sessionId)
                    .productOption(option2)
                    .count(2)
                    .build();

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(sessionId))
                    .willReturn(Arrays.asList(guestCart1, guestCart2));
            given(cartRepository.findByMemberAndProductOption(testMember, testProductOption))
                    .willReturn(Optional.of(testCart)); // 이미 회원 장바구니에 있음
            given(cartRepository.findByMemberAndProductOption(testMember, option2))
                    .willReturn(Optional.empty()); // 회원 장바구니에 없음
            given(cartRepository.findByMemberWithProduct(testMember))
                    .willReturn(Arrays.asList(testCart, guestCart2));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(sessionId, 1L);

            // then
            assertThat(testCart.getCount()).isEqualTo(5); // 2 + 3
            assertThat(guestCart2.getMember()).isEqualTo(testMember);
            assertThat(guestCart2.getSessionId()).isNull();
            verify(cartRepository).delete(guestCart1);
        }

        @Test
        @DisplayName("병합할 비회원 장바구니가 없으면 회원 장바구니만 반환")
        void mergeWithEmptyGuestCart() {
            // given
            String sessionId = "test-session-id";

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(cartRepository.findBySessionId(sessionId)).willReturn(List.of());
            given(cartRepository.findByMemberWithProduct(testMember)).willReturn(List.of(testCart));

            // when
            List<CartDto.Response> results = cartService.mergeGuestCartToMember(sessionId, 1L);

            // then
            assertThat(results).hasSize(1);
            verify(cartRepository, never()).delete(any());
        }

        @Test
        @DisplayName("세션 ID 없이 병합 시도 시 예외 발생")
        void mergeWithoutSessionId() {
            // when & then
            assertThatThrownBy(() -> cartService.mergeGuestCartToMember(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("세션 ID가 필요합니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 병합 시 예외 발생")
        void mergeWithInvalidMember() {
            // given
            String sessionId = "test-session-id";
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.mergeGuestCartToMember(sessionId, 999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }
    }
}
