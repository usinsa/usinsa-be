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
import org.junit.jupiter.api.Nested;
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
    private Product testProduct;
    private ProductOption testProductOption;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        session = new MockHttpSession();

        // 회원 생성
        testMember = Member.builder()
                .usinaId("testuser")
                .name("테스트유저")
                .email("test@example.com")
                .password("password")
                .phone("01012341234")
                .nickname("tester")
                .build();
        testMember = memberRepository.save(testMember);

        // 카테고리 생성
        Category category = Category.builder()
                .name("상의")
                .build();
        category = categoryRepository.save(category);

        // 상품 생성
        testProduct = Product.builder()
                .name("오버핏 티셔츠")
                .brandName("무신사 스탠다드")
                .price(29900L)
                .category(category)
                .build();
        testProduct = productRepository.save(testProduct);

        // 상품 옵션 생성
        testProductOption = ProductOption.builder()
                .optionName("Black / L")
                .stock(50)
                .product(testProduct)
                .build();
        testProductOption = productOptionRepository.save(testProductOption);
    }

    @Nested
    @DisplayName("비회원 장바구니")
    class GuestCart {

        @Test
        @DisplayName("비회원이 장바구니에 상품 추가 (상품 정보 포함)")
        void createGuestCart() throws Exception {
            // given
            CartDto.GuestCreateReq request = CartDto.GuestCreateReq.builder()
                    .productOptionId(testProductOption.getId())
                    .count(2)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/carts/guest")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productOptionId").value(testProductOption.getId()))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.guest").value(true))
                    .andExpect(jsonPath("$.memberId").doesNotExist())
                    .andExpect(jsonPath("$.productInfo").exists())
                    .andExpect(jsonPath("$.productInfo.productName").value("오버핏 티셔츠"))
                    .andExpect(jsonPath("$.productInfo.brandName").value("무신사 스탠다드"))
                    .andExpect(jsonPath("$.productInfo.price").value(29900))
                    .andExpect(jsonPath("$.productInfo.optionName").value("Black / L"))
                    .andExpect(jsonPath("$.productInfo.stock").value(50));
        }

        @Test
        @DisplayName("비회원 장바구니 조회 시 상품 정보 포함")
        void getGuestCart() throws Exception {
            // given
            Cart guestCart = Cart.builder()
                    .sessionId(session.getId())
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cartRepository.save(guestCart);

            // when & then
            mockMvc.perform(get("/api/v1/carts/guest")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productInfo").exists())
                    .andExpect(jsonPath("$[0].productInfo.productName").value("오버핏 티셔츠"))
                    .andExpect(jsonPath("$[0].productInfo.brandName").value("무신사 스탠다드"));
        }

        @Test
        @DisplayName("동일 상품 추가 시 수량 증가")
        void addSameProductToGuestCart() throws Exception {
            // given - 먼저 장바구니에 상품 추가 (수량 2)
            CartDto.GuestCreateReq firstRequest = CartDto.GuestCreateReq.builder()
                    .productOptionId(testProductOption.getId())
                    .count(2)
                    .build();

            mockMvc.perform(post("/api/v1/carts/guest")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)));

            // when - 동일 상품 다시 추가 (수량 3)
            CartDto.GuestCreateReq secondRequest = CartDto.GuestCreateReq.builder()
                    .productOptionId(testProductOption.getId())
                    .count(3)
                    .build();

            // then
            mockMvc.perform(post("/api/v1/carts/guest")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5)); // 2 + 3
        }

        @Test
        @DisplayName("비회원 장바구니 전체 삭제")
        void deleteGuestCart() throws Exception {
            // given
            Cart guestCart = Cart.builder()
                    .sessionId(session.getId())
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cartRepository.save(guestCart);

            // when & then
            mockMvc.perform(delete("/api/v1/carts/guest")
                            .session(session))
                    .andExpect(status().isNoContent());

            assertThat(cartRepository.findBySessionId(session.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원 장바구니")
    class MemberCart {

        @Test
        @DisplayName("회원이 장바구니에 상품 추가 (상품 정보 포함)")
        void createMemberCart() throws Exception {
            // given
            CartDto.CreateReq request = CartDto.CreateReq.builder()
                    .memberId(testMember.getId())
                    .productOptionId(testProductOption.getId())
                    .count(1)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/carts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(testMember.getId()))
                    .andExpect(jsonPath("$.guest").value(false))
                    .andExpect(jsonPath("$.productInfo").exists())
                    .andExpect(jsonPath("$.productInfo.productName").value("오버핏 티셔츠"));
        }

        @Test
        @DisplayName("회원 장바구니 조회 시 상품 정보 포함")
        void getMemberCarts() throws Exception {
            // given
            Cart memberCart = Cart.builder()
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cartRepository.save(memberCart);

            // when & then
            mockMvc.perform(get("/api/v1/carts/member/" + testMember.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productInfo").exists())
                    .andExpect(jsonPath("$[0].productInfo.productName").value("오버핏 티셔츠"));
        }

        @Test
        @DisplayName("장바구니 수량 수정")
        void updateCart() throws Exception {
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
            mockMvc.perform(put("/api/v1/carts/" + cart.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5))
                    .andExpect(jsonPath("$.productInfo").exists());
        }

        @Test
        @DisplayName("장바구니 단건 삭제")
        void deleteCart() throws Exception {
            // given
            Cart cart = Cart.builder()
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cart = cartRepository.save(cart);

            // when & then
            mockMvc.perform(delete("/api/v1/carts/" + cart.getId()))
                    .andExpect(status().isNoContent());

            assertThat(cartRepository.findById(cart.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("장바구니 병합")
    class MergeCart {

        @Test
        @DisplayName("비회원 장바구니를 회원 장바구니로 병합")
        void mergeGuestCart() throws Exception {
            // given
            Cart guestCart = Cart.builder()
                    .sessionId(session.getId())
                    .productOption(testProductOption)
                    .count(3)
                    .build();
            cartRepository.save(guestCart);

            // when
            mockMvc.perform(post("/api/v1/carts/merge/" + testMember.getId())
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].memberId").value(testMember.getId()))
                    .andExpect(jsonPath("$[0].count").value(3))
                    .andExpect(jsonPath("$[0].guest").value(false))
                    .andExpect(jsonPath("$[0].productInfo").exists());

            // then - 비회원 장바구니가 비어있는지 확인
            mockMvc.perform(get("/api/v1/carts/guest")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("동일 상품이 있을 때 병합하면 수량 합산")
        void mergeWithExistingProduct() throws Exception {
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

            // when
            mockMvc.perform(post("/api/v1/carts/merge/" + testMember.getId())
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].count").value(5)) // 2 + 3
                    .andExpect(jsonPath("$[0].productInfo").exists());

            // then - 회원 장바구니 항목이 1개만 있는지 확인
            mockMvc.perform(get("/api/v1/carts/member/" + testMember.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("비회원 장바구니가 비어있으면 회원 장바구니만 반환")
        void mergeEmptyGuestCart() throws Exception {
            // given - 회원 장바구니만 존재
            Cart memberCart = Cart.builder()
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cartRepository.save(memberCart);

            // when & then
            mockMvc.perform(post("/api/v1/carts/merge/" + testMember.getId())
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].memberId").value(testMember.getId()));
        }

        @Test
        @DisplayName("여러 상품이 있을 때 병합")
        void mergeMultipleProducts() throws Exception {
            // given - 회원 장바구니에 상품1
            Cart memberCart = Cart.builder()
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cartRepository.save(memberCart);

            // given - 다른 상품 옵션 생성
            ProductOption option2 = ProductOption.builder()
                    .optionName("White / M")
                    .stock(30)
                    .product(testProduct)
                    .build();
            option2 = productOptionRepository.save(option2);

            // given - 비회원 장바구니에 상품1(수량 증가)과 상품2(신규)
            Cart guestCart1 = Cart.builder()
                    .sessionId(session.getId())
                    .productOption(testProductOption)
                    .count(1)
                    .build();
            cartRepository.save(guestCart1);

            Cart guestCart2 = Cart.builder()
                    .sessionId(session.getId())
                    .productOption(option2)
                    .count(3)
                    .build();
            cartRepository.save(guestCart2);

            // when
            mockMvc.perform(post("/api/v1/carts/merge/" + testMember.getId())
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            // then - 회원 장바구니 확인
            mockMvc.perform(get("/api/v1/carts/member/" + testMember.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[?(@.productInfo.optionName=='Black / L')].count").value(3)) // 2 + 1
                    .andExpect(jsonPath("$[?(@.productInfo.optionName=='White / M')].count").value(3));
        }
    }

    @Nested
    @DisplayName("장바구니 단건 조회")
    class GetCartById {

        @Test
        @DisplayName("ID로 장바구니 조회 시 상품 정보 포함")
        void getCartById() throws Exception {
            // given
            Cart cart = Cart.builder()
                    .member(testMember)
                    .productOption(testProductOption)
                    .count(2)
                    .build();
            cart = cartRepository.save(cart);

            // when & then
            mockMvc.perform(get("/api/v1/carts/" + cart.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cart.getId()))
                    .andExpect(jsonPath("$.productInfo").exists())
                    .andExpect(jsonPath("$.productInfo.productName").value("오버핏 티셔츠"))
                    .andExpect(jsonPath("$.productInfo.brandName").value("무신사 스탠다드"))
                    .andExpect(jsonPath("$.productInfo.price").value(29900))
                    .andExpect(jsonPath("$.productInfo.optionName").value("Black / L"));
        }
    }
}
