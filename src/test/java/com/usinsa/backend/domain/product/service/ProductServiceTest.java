package com.usinsa.backend.domain.product.service;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.category.repository.CategoryRepository;
import com.usinsa.backend.domain.product.dto.ProductDto;
import com.usinsa.backend.domain.product.dto.ProductOptionDto;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.entity.ProductOption;
import com.usinsa.backend.domain.product.repository.ProductOptionRepository;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductOptionRepository optionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Category testCategory;
    private Product testProduct;
    private ProductDto.CreateReq createReq;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("상의")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .category(testCategory)
                .name("반팔 티셔츠")
                .brandName("나이키")
                .price(39000L)
                .likeCount(100)
                .clickCount(500)
                .build();

        createReq = ProductDto.CreateReq.builder()
                .categoryId(1L)
                .name("반팔 티셔츠")
                .brand("나이키")
                .price(39000L)
                .build();
    }

    @Nested
    @DisplayName("상품 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("정상적으로 상품을 생성한다")
        void create_Success() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.of(testCategory));
            given(productRepository.save(any(Product.class))).willReturn(testProduct);

            // when
            ProductDto.Response result = productService.create(createReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("반팔 티셔츠");
            assertThat(result.getBrandName()).isEqualTo("나이키");
            assertThat(result.getPrice()).isEqualTo(39000L);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("카테고리가 없으면 상품 생성에 실패한다")
        void create_CategoryNotFound_Fail() {
            // given
            given(categoryRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.create(createReq))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("카테고리를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("상품 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("ID로 상품을 조회한다")
        void findById_Success() {
            // given
            given(productRepository.findWithCategoryAndOptionsById(anyLong()))
                    .willReturn(Optional.of(testProduct));

            // when
            ProductDto.Response result = productService.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("반팔 티셔츠");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void findById_NotFound_Fail() {
            // given
            given(productRepository.findWithCategoryAndOptionsById(anyLong()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.findById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품을 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("모든 상품을 조회한다")
        void findAll_Success() {
            // given
            Product product2 = Product.builder()
                    .id(2L)
                    .category(testCategory)
                    .name("긴팔 티셔츠")
                    .brandName("아디다스")
                    .price(49000L)
                    .likeCount(50)
                    .clickCount(300)
                    .build();

            given(productRepository.findAll()).willReturn(Arrays.asList(testProduct, product2));

            // when
            List<ProductDto.Response> results = productService.findAll();

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo("반팔 티셔츠");
            assertThat(results.get(1).getName()).isEqualTo("긴팔 티셔츠");
        }
    }

    @Nested
    @DisplayName("상품 수정 테스트")
    class UpdateTest {

        @Test
        @DisplayName("정상적으로 상품을 수정한다")
        void update_Success() {
            // given
            ProductDto.CreateReq updateReq = ProductDto.CreateReq.builder()
                    .name("수정된 티셔츠")
                    .brand("푸마")
                    .price(45000L)
                    .build();

            given(productRepository.findById(anyLong())).willReturn(Optional.of(testProduct));

            // when
            ProductDto.Response result = productService.update(1L, updateReq);

            // then
            assertThat(result).isNotNull();
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("존재하지 않는 상품 수정 시 예외가 발생한다")
        void update_NotFound_Fail() {
            // given
            given(productRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.update(999L, createReq))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("상품 삭제 테스트")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 상품을 삭제한다")
        void delete_Success() {
            // given
            given(productRepository.findById(anyLong())).willReturn(Optional.of(testProduct));
            doNothing().when(productRepository).delete(any(Product.class));

            // when
            productService.delete(1L);

            // then
            verify(productRepository).delete(testProduct);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("존재하지 않는 상품 삭제 시 예외가 발생한다")
        void delete_NotFound_Fail() {
            // given
            given(productRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.delete(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("상품 옵션 추가 테스트")
    class AddOptionTest {

        @Test
        @DisplayName("정상적으로 옵션을 추가한다")
        void addOption_Success() {
            // given
            ProductOptionDto.CreateReq optionReq = ProductOptionDto.CreateReq.builder()
                    .optionName("M 사이즈")
                    .stock(100)
                    .build();

            ProductOption savedOption = ProductOption.builder()
                    .id(1L)
                    .optionName("M 사이즈")
                    .stock(100)
                    .product(testProduct)
                    .build();

            given(productRepository.findById(anyLong())).willReturn(Optional.of(testProduct));
            given(optionRepository.save(any(ProductOption.class))).willReturn(savedOption);

            // when
            ProductOptionDto.Response result = productService.addOption(1L, optionReq);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOptionName()).isEqualTo("M 사이즈");
            assertThat(result.getStock()).isEqualTo(100);
        }
    }
}
