package com.usinsa.backend.domain.cart.service;

import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.repository.CartRepository;
import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("세션 만료 시 비회원 장바구니 자동 삭제 시뮬레이션")
    void testSessionCleanup() {
        // Given: 비회원이 장바구니에 상품 추가
        String sessionId = UUID.randomUUID().toString();
        CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                .productOptionId(testProductOption.getId())
                .count(2)
                .build();

        cartService.createGuestCart(request, sessionId);

        // 장바구니가 생성되었는지 확인
        List<CartDto.Response> guestCarts = cartService.findBySessionId(sessionId);
        assertThat(guestCarts).hasSize(1);

        // When: 세션 만료로 인한 장바구니 삭제 (세션 리스너 동작 시뮬레이션)
        int deletedCount = cartService.deleteGuestCartBySessionId(sessionId);

        // Then: 비회원 장바구니가 삭제되어야 함
        assertThat(deletedCount).isEqualTo(1);

        List<CartDto.Response> cartsAfterCleanup = cartService.findBySessionId(sessionId);
        assertThat(cartsAfterCleanup).isEmpty();
    }

    @Test
    @DisplayName("여러 비회원 장바구니 항목 동시 삭제")
    void testMultipleGuestCartCleanup() {
        // Given: 비회원이 여러 상품을 장바구니에 추가
        String sessionId = UUID.randomUUID().toString();

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
                sessionId
        );
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(option2.getId()).count(2).build(),
                sessionId
        );
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(option3.getId()).count(3).build(),
                sessionId
        );

        assertThat(cartService.findBySessionId(sessionId)).hasSize(3);

        // When: 세션 만료
        int deletedCount = cartService.deleteGuestCartBySessionId(sessionId);

        // Then: 모든 비회원 장바구니 항목이 삭제되어야 함
        assertThat(deletedCount).isEqualTo(3);
        assertThat(cartService.findBySessionId(sessionId)).isEmpty();
    }

    @Test
    @DisplayName("로그인 후 병합된 장바구니는 세션 만료 시 삭제되지 않음")
    void testMergedCartNotDeletedOnSessionExpiry() {
        // Given: 비회원이 장바구니에 상품 추가
        String sessionId = UUID.randomUUID().toString();
        cartService.createGuestCart(
                CartDto.GuestCreateReq.builder().productOptionId(testProductOption.getId()).count(2).build(),
                sessionId
        );

        // 비회원이 로그인하여 장바구니 병합
        cartService.mergeGuestCartToMember(sessionId, testMember.getId());

        // 병합 후 비회원 장바구니는 이미 없어야 함
        List<CartDto.Response> guestCartsAfterMerge = cartService.findBySessionId(sessionId);
        assertThat(guestCartsAfterMerge).isEmpty();

        // 회원 장바구니에는 존재해야 함
        List<CartDto.Response> memberCarts = cartService.findByMemberId(testMember.getId());
        assertThat(memberCarts).hasSize(1);

        // When: 세션 만료 (더 이상 삭제할 비회원 장바구니 없음)
        int deletedCount = cartService.deleteGuestCartBySessionId(sessionId);

        // Then: 삭제된 항목 없음 (이미 병합되어 비회원 장바구니가 없음)
        assertThat(deletedCount).isEqualTo(0);

        // 회원 장바구니는 그대로 유지
        List<CartDto.Response> memberCartsAfterCleanup = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsAfterCleanup).hasSize(1);
    }

    @Test
    @DisplayName("유효하지 않은 세션 ID로 삭제 시도")
    void testCleanupWithInvalidSessionId() {
        // Given: 유효하지 않은 세션 ID
        String invalidSessionId = null;

        // When & Then: 예외 발생 없이 0 반환
        int deletedCount = cartService.deleteGuestCartBySessionId(invalidSessionId);
        assertThat(deletedCount).isEqualTo(0);

        // 빈 문자열도 마찬가지
        deletedCount = cartService.deleteGuestCartBySessionId("");
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 삭제 시도")
    void testCleanupWithNonExistentSessionId() {
        // Given: 존재하지 않는 세션 ID
        String nonExistentSessionId = UUID.randomUUID().toString();

        // When: 삭제 시도
        int deletedCount = cartService.deleteGuestCartBySessionId(nonExistentSessionId);

        // Then: 삭제된 항목 없음
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("회원 장바구니는 세션 만료의 영향을 받지 않음")
    void testMemberCartNotAffectedBySessionCleanup() {
        // Given: 회원이 직접 장바구니에 상품 추가
        CartDto.CreateReq memberRequest = CartDto.CreateReq.builder()
                .memberId(testMember.getId())
                .productOptionId(testProductOption.getId())
                .count(3)
                .build();
        cartService.create(memberRequest);

        List<CartDto.Response> memberCartsBefore = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsBefore).hasSize(1);

        // When: 임의의 세션 ID로 세션 만료 시뮬레이션
        String randomSessionId = UUID.randomUUID().toString();
        int deletedCount = cartService.deleteGuestCartBySessionId(randomSessionId);

        // Then: 회원 장바구니는 영향 없음
        assertThat(deletedCount).isEqualTo(0);
        List<CartDto.Response> memberCartsAfter = cartService.findByMemberId(testMember.getId());
        assertThat(memberCartsAfter).hasSize(1);
        assertThat(memberCartsAfter.get(0).getCount()).isEqualTo(3);
    }
}
