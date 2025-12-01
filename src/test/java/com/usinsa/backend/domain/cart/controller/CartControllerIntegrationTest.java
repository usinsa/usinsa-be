package com.usinsa.backend.domain.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usinsa.backend.domain.cart.dto.CartDto;
import com.usinsa.backend.domain.cart.entity.Cart;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 장바구니 통합 테스트
 * 
 * 주의: 이 테스트는 실제 데이터베이스와 의존성이 필요합니다.
 * @SpringBootTest를 사용하여 전체 애플리케이션 컨텍스트를 로드합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CartController 통합 테스트")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    private Member testMember;
    private ProductOption testProductOption;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        cartRepository.deleteAll();
        
        // 세션 생성
        session = new MockHttpSession();
        
        // 테스트 회원 생성
        testMember = Member.builder()
                .usinaId("testuser")
                .name("테스트유저")
                .email("test@example.com")
                .password("password")
                .phone("01012341234")
                .nickname("tester")
                .build();
        testMember = memberRepository.save(testMember);

        // Category 생성
        Category category = Category.builder()
                .name("테스트카테고리")
                .build();
        category = categoryRepository.save(category);

        // Product 생성 시 category 넣기
        Product testProduct = Product.builder()
                .name("테스트상품")
                .price(10000L)
                .brandName("테스트브랜드")
                .category(category)
                .build();
        testProduct = productRepository.save(testProduct);

        // 테스트 상품 옵션 생성
        testProductOption = ProductOption.builder()
                .optionName("M 사이즈")
                .stock(100)
                .product(testProduct)
                .build();
        testProductOption = productOptionRepository.save(testProductOption);
    }

    @Test
    @DisplayName("비회원이 장바구니에 상품을 추가할 수 있다")
    void createGuestCart_Success() throws Exception {
        // given
        CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                .productOptionId(testProductOption.getId())
                .count(2)
                .build();

        // when & then
        mockMvc.perform(post("/carts/guest")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productOptionId").value(testProductOption.getId()))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.guest").value(true))
                .andExpect(jsonPath("$.memberId").doesNotExist());
    }

    @Test
    @DisplayName("비회원이 장바구니를 조회할 수 있다")
    void getGuestCart_Success() throws Exception {
        // given
        Cart guestCart = Cart.builder()
                .sessionId(session.getId())
                .productOption(testProductOption)
                .count(2)
                .build();
        cartRepository.save(guestCart);

        // when & then
        mockMvc.perform(get("/carts/guest")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productOptionId").value(testProductOption.getId()))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].guest").value(true));
    }

    @Test
    @DisplayName("회원이 장바구니에 상품을 추가할 수 있다")
    void createMemberCart_Success() throws Exception {
        // given
        CartDto.CreateReq request = CartDto.CreateReq.builder()
                .memberId(testMember.getId())
                .productOptionId(testProductOption.getId())
                .count(1)
                .build();

        // when & then
        mockMvc.perform(post("/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(testMember.getId()))
                .andExpect(jsonPath("$.productOptionId").value(testProductOption.getId()))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.guest").value(false));
    }

    @Test
    @DisplayName("비회원 장바구니를 회원 장바구니로 병합할 수 있다")
    void mergeGuestCart_Success() throws Exception {
        // given - 비회원 장바구니에 상품 추가
        Cart guestCart = Cart.builder()
                .sessionId(session.getId())
                .productOption(testProductOption)
                .count(3)
                .build();
        cartRepository.save(guestCart);

        // when - 병합 API 호출
        mockMvc.perform(post("/carts/merge/" + testMember.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(testMember.getId()))
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[0].guest").value(false));

        // then - 비회원 장바구니가 삭제되었는지 확인
        mockMvc.perform(get("/carts/guest")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("동일 상품이 있을 때 병합하면 수량이 합산된다")
    void mergeGuestCart_WithExistingProduct_IncrementCount() throws Exception {
        // given - 회원 장바구니에 상품 추가 (수량 2)
        Cart memberCart = Cart.builder()
                .member(testMember)
                .productOption(testProductOption)
                .count(2)
                .build();
        cartRepository.save(memberCart);

        // given - 비회원 장바구니에 동일 상품 추가 (수량 3)
        Cart guestCart = Cart.builder()
                .sessionId(session.getId())
                .productOption(testProductOption)
                .count(3)
                .build();
        cartRepository.save(guestCart);

        // when - 병합
        mockMvc.perform(post("/carts/merge/" + testMember.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(5)); // 2 + 3

        // then - 장바구니 항목이 1개만 있는지 확인
        mockMvc.perform(get("/carts/member/" + testMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("장바구니 수량을 수정할 수 있다")
    void updateCart_Success() throws Exception {
        // given
        Cart cart = Cart.builder()
                .member(testMember)
                .productOption(testProductOption)
                .count(2)
                .build();
        cart = cartRepository.save(cart);

        CartDto.UpdateReq updateReq = CartDto.UpdateReq.builder()
                .count(5)
                .build();

        // when & then
        mockMvc.perform(put("/carts/" + cart.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("장바구니 항목을 삭제할 수 있다")
    void deleteCart_Success() throws Exception {
        // given
        Cart cart = Cart.builder()
                .member(testMember)
                .productOption(testProductOption)
                .count(2)
                .build();
        cart = cartRepository.save(cart);

        // when & then
        mockMvc.perform(delete("/carts/" + cart.getId()))
                .andExpect(status().isNoContent());

        // 삭제 확인
        assertThat(cartRepository.findById(cart.getId())).isEmpty();
    }

    @Test
    @DisplayName("비회원 장바구니를 전체 삭제할 수 있다")
    void deleteGuestCart_Success() throws Exception {
        // given
        Cart guestCart = Cart.builder()
                .sessionId(session.getId())
                .productOption(testProductOption)
                .count(2)
                .build();
        cartRepository.save(guestCart);

        // when & then
        mockMvc.perform(delete("/carts/guest")
                        .session(session))
                .andExpect(status().isNoContent());

        // 삭제 확인
        assertThat(cartRepository.findBySessionId(session.getId())).isEmpty();
    }
}
