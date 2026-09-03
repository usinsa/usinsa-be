package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.port.ProductVectorSearchPort;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 비회원 장바구니 정리(cleanup) 통합 테스트
 *
 * 과거에는 HttpSession 만료 리스너가 CartService.deleteGuestCartBySessionId(sessionId)를 호출해
 * 세션 종료 시 DB 행을 즉시 정리했지만, 현재는 guestId(Redis) 기반 TTL(7일) 방식으로 대체되어
 * 해당 메서드는 더 이상 존재하지 않는다. 지금은 CartService.deleteGuestCart(guestId)가
 * DB 행과 Redis 키를 함께 제거하는 역할을 하므로, 이를 기준으로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartSessionCleanupTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private ProductIndexPort productIndexPort;

    @MockBean
    private ProductSearchPort productSearchPort;

    @MockBean
    private ProductVectorSearchPort productVectorSearchPort;

    private ProductOption testProductOption;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        Category category = Category.builder()
                .name("테스트카테고리")
                .build();
        categoryRepository.save(category);

        Product product = Product.builder()
                .category(category)
                .name("테스트상품")
                .brandName("테스트브랜드")
                .price(10000L)
                .build();
        productRepository.save(product);

        testProductOption = ProductOption.builder()
                .product(product)
                .optionName("기본옵션")
                .stock(100)
                .build();
        productOptionRepository.save(testProductOption);

        testMember = Member.builder()
                .usinaId("testuser")
                .email("test@test.com")
                .password("password")
                .name("테스트유저")
                .nickname("테스트")
                .phone("010-1234-5678")
                .build();
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("guestId 기반 비회원 장바구니 정리")
    void testGuestCartCleanup() {
        // Given: 비회원이 장바구니에 상품 추가
        String guestId = UUID.randomUUID().toString();
        CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                .productOptionId(testProductOption.getId())
                .count(2)
                .build();

        cartService.createGuestCart(request, guestId);

        // 장바구니가 생성되었는지 확인
        List<CartDto.Response> guestCarts = cartService.findByGuestId(guestId);
        assertThat(guestCarts).hasSize(1);

        // When: 비회원 장바구니 정리
        cartService.deleteGuestCart(guestId);

        // Then: 비회원 장바구니가 삭제되어야 함
        List<CartDto.Response> cartsAfterCleanup = cartService.findByGuestId(guestId);
        assertThat(cartsAfterCleanup).isEmpty();
    }

    @Test
    @DisplayName("여러 비회원 장바구니 항목 동시 삭제")
    void testMultipleGuestCartCleanup() {
        // Given: 비회원이 여러 상품을 장바구니에 추가
        String guestId = UUID.randomUUID().toString();

        // 상품 옵션 3개 추가 생성
        ProductOption option2 = ProductOption.builder()
                .product(testProductOption.getProduct())
                .optionName("옵션2")
                .stock(50)
                .build();
        productOptionRepository.save(option2);

        ProductOption option3 = ProductOption.builder()
                .product(testProductOption.getProduct())
                .optionName("옵션3")
                .stock(30)
                .build();
        productOptionRepository.save(option3);

        // 세 개의 상품을 장바구니에 추가
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(testProductOption.getId()).count(1).build(),
                guestId
        );
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(option2.getId()).count(2).build(),
                guestId
        );
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(option3.getId()).count(3).build(),
                guestId
        );

        assertThat(cartService.findByGuestId(guestId)).hasSize(3);

        // When: 정리
        cartService.deleteGuestCart(guestId);

        // Then: 모든 비회원 장바구니 항목이 삭제되어야 함
        assertThat(cartService.findByGuestId(guestId)).isEmpty();
    }

    @Test
    @DisplayName("로그인 후 병합된 장바구니는 재정리해도 영향받지 않음")
    void testMergedCartNotAffectedByCleanup() {
        // Given: 비회원이 장바구니에 상품 추가
        String guestId = UUID.randomUUID().toString();
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(testProductOption.getId()).count(2).build(),
                guestId
        );

        // 비회원이 로그인하여 장바구니 병합
        cartService.mergeGuestCartToMember(guestId, testMember.getId());

        // 병합 후 비회원 장바구니는 이미 없어야 함
        List<CartDto.Response> guestCartsAfterMerge = cartService.findByGuestId(guestId);
        assertThat(guestCartsAfterMerge).isEmpty();

        // 회원 장바구니에는 존재해야 함
        List<CartDto.Response> memberCarts = cartService.findByMemberId(testMember.getId());
        assertThat(memberCarts).hasSize(1);

        // When: 이미 병합되어 비어있는 guestId를 다시 정리해도 예외가 발생하지 않아야 함
        cartService.deleteGuestCart(guestId);

        // Then: 회원 장바구니는 그대로 유지
        List<CartDto.Response> memberCartsAfterCleanup = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsAfterCleanup).hasSize(1);
    }

    @Test
    @DisplayName("유효하지 않은 guestId로 정리 시도 시 예외 발생")
    void testCleanupWithInvalidGuestId() {
        // When & Then: null/빈 문자열은 예외를 발생시킨다
        assertThatThrownBy(() -> cartService.deleteGuestCart(null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SESSION_ID_REQUIRED.getMessage());

        assertThatThrownBy(() -> cartService.deleteGuestCart(""))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SESSION_ID_REQUIRED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 guestId로 정리 시도해도 예외 없이 완료된다")
    void testCleanupWithNonExistentGuestId() {
        // Given: 존재하지 않는 guestId
        String nonExistentGuestId = UUID.randomUUID().toString();

        // When & Then: 삭제할 데이터가 없어도 예외 없이 완료된다
        cartService.deleteGuestCart(nonExistentGuestId);
        assertThat(cartService.findByGuestId(nonExistentGuestId)).isEmpty();
    }

    @Test
    @DisplayName("회원 장바구니는 비회원 장바구니 정리의 영향을 받지 않음")
    void testMemberCartNotAffectedByGuestCleanup() {
        // Given: 회원이 직접 장바구니에 상품 추가
        CartDto.CreateReq memberRequest = CartDto.CreateReq.builder()
                .memberId(testMember.getId())
                .productOptionId(testProductOption.getId())
                .count(3)
                .build();
        cartService.create(memberRequest);

        List<CartDto.Response> memberCartsBefore = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsBefore).hasSize(1);

        // When: 임의의 guestId로 비회원 장바구니 정리
        String randomGuestId = UUID.randomUUID().toString();
        cartService.deleteGuestCart(randomGuestId);

        // Then: 회원 장바구니는 영향 없음
        List<CartDto.Response> memberCartsAfter = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsAfter).hasSize(1);
        assertThat(memberCartsAfter.get(0).getCount()).isEqualTo(3);
    }
}
