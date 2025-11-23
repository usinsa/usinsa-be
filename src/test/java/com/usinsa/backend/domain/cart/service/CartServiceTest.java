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
    private ProductOption testProductOption;
    private Cart testCart;
    private CartDto.CreateReq createReq;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .name("테스트유저")
                .build();

        Product testProduct = Product.builder()
                .id(1L)
                .name("테스트상품")
                .build();

        testProductOption = ProductOption.builder()
                .id(1L)
                .optionName("M 사이즈")
                .stock(100)
                .product(testProduct)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .member(testMember)
                .productOption(testProductOption)
                .count(2)
                .build();

        createReq = CartDto.CreateReq.builder()
                .memberId(1L)
                .productOptionId(1L)
                .count(2)
                .build();
    }

    @Nested
    @DisplayName("장바구니 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 장바구니에 상품을 추가한다")
        void create_Success() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(testMember));
            given(productOptionRepository.findById(anyLong())).willReturn(Optional.of(testProductOption));
            given(cartRepository.save(any(Cart.class))).willReturn(testCart);

            // when
            CartDto.Response result = cartService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMemberId()).isEqualTo(1L);
            assertThat(result.getProductOptionId()).isEqualTo(1L);
            assertThat(result.getCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("회원이 없으면 장바구니 추가에 실패한다")
        void create_MemberNotFound_Fail() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.create(createReq))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("상품 옵션이 없으면 장바구니 추가에 실패한다")
        void create_ProductOptionNotFound_Fail() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(testMember));
            given(productOptionRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.create(createReq))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("상품 옵션이 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 장바구니를 조회한다")
        void findById_Success() {
            // given
            given(cartRepository.findById(anyLong())).willReturn(Optional.of(testCart));

            // when
            CartDto.Response result = cartService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 조회 시 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(cartRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }

        @Test
        @DisplayName("모든 장바구니를 조회한다")
        void findAll_Success() {
            // given
            Cart cart2 = Cart.builder()
                    .id(2L)
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(3)
                    .build();

            given(cartRepository.findAll()).willReturn(Arrays.asList(testCart, cart2));

            // when
            List<CartDto.Response> results = cartService.findAll();

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("장바구니 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 장바구니 수량을 수정한다")
        void update_Success() {
            // given
            CartDto.UpdateReq updateReq = CartDto.UpdateReq.builder()
                    .count(5)
                    .build();

            given(cartRepository.findById(anyLong())).willReturn(Optional.of(testCart));

            // when
            CartDto.Response result = cartService.update(1L, updateReq);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            CartDto.UpdateReq updateReq = CartDto.UpdateReq.builder().count(5).build();
            given(cartRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> cartService.update(999L, updateReq))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("장바구니 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 장바구니 항목을 삭제한다")
        void delete_Success() {
            // given
            given(cartRepository.existsById(anyLong())).willReturn(true);
            doNothing().when(cartRepository).deleteById(anyLong());

            // when
            cartService.delete(1L);

            // then
            verify(cartRepository).deleteById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 장바구니 삭제 시 예외가 발생한다")
        void delete_NotFound_Fail() {
            // given
            given(cartRepository.existsById(anyLong())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> cartService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("장바구니가 존재하지 않습니다.");
        }
    }
}
