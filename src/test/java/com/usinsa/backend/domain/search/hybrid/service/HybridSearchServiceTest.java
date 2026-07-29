package com.usinsa.backend.domain.search.hybrid.service;

import com.usinsa.backend.domain.category.entity.Category;
import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.embedding.client.EmbeddingClient;
import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.port.ProductVectorSearchPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import com.usinsa.backend.domain.search.vector.dto.VectorSearchResultDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @InjectMocks
    private HybridSearchService hybridSearchService;

    @Mock
    private ProductSearchPort productSearchPort;

    @Mock
    private ProductVectorSearchPort productVectorSearchPort;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private ProductRepository productRepository;

    @Nested
    @DisplayName("RRF 병합 테스트")
    class RrfMergeTest {

        @Test
        @DisplayName("키워드와 벡터 양쪽에 모두 등장한 상품은 점수가 합산되어 상위에 랭크된다")
        void search_ProductInBothLists_ScoreSummed() {
            // given
            ProductSearchDto keywordHit = ProductSearchDto.builder()
                    .id(1L).name("반팔 티셔츠").brandName("나이키").categoryName("상의").price(39000L)
                    .build();
            ProductSearchDto keywordOnly = ProductSearchDto.builder()
                    .id(2L).name("청바지").brandName("리바이스").categoryName("하의").price(59000L)
                    .build();

            given(productSearchPort.search(anyString()))
                    .willReturn(List.of(keywordHit, keywordOnly)); // rank 1, 2

            given(embeddingClient.embedQuery(anyString())).willReturn(new float[]{0.1f, 0.2f});

            given(productVectorSearchPort.search(any(float[].class), anyInt()))
                    .willReturn(List.of(
                            VectorSearchResultDto.builder().productId(1L).similarity(0.95).build() // rank 1
                    ));

            // when
            List<RankedProductDto> result = hybridSearchService.search("티셔츠", 5);

            // then: 양쪽에 다 있는 productId=1이 1등이어야 한다 (1/61 + 1/61 > 1/62)
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getProductId()).isEqualTo(1L);
            assertThat(result.get(0).getRrfScore()).isGreaterThan(result.get(1).getRrfScore());
        }

        @Test
        @DisplayName("벡터 검색에서만 발견된 상품은 ProductRepository로 상세 정보를 채운다")
        void search_VectorOnlyHit_FetchesFromRepository() {
            // given
            given(productSearchPort.search(anyString())).willReturn(List.of());
            given(embeddingClient.embedQuery(anyString())).willReturn(new float[]{0.1f});
            given(productVectorSearchPort.search(any(float[].class), anyInt()))
                    .willReturn(List.of(
                            VectorSearchResultDto.builder().productId(3L).similarity(0.9).build()
                    ));

            Category category = Category.builder().id(1L).name("아우터").build();
            Product product = Product.builder()
                    .id(3L).name("코트").brandName("자라").category(category).price(120000L)
                    .build();
            given(productRepository.findById(3L)).willReturn(Optional.of(product));

            // when
            List<RankedProductDto> result = hybridSearchService.search("코트", 5);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("코트");
            assertThat(result.get(0).getCategoryName()).isEqualTo("아우터");
        }

        @Test
        @DisplayName("topK를 초과하는 결과는 잘라낸다")
        void search_LimitsToTopK() {
            // given
            List<ProductSearchDto> manyHits = List.of(
                    ProductSearchDto.builder().id(1L).name("A").build(),
                    ProductSearchDto.builder().id(2L).name("B").build(),
                    ProductSearchDto.builder().id(3L).name("C").build()
            );
            given(productSearchPort.search(anyString())).willReturn(manyHits);
            given(embeddingClient.embedQuery(anyString())).willReturn(new float[]{0.1f});
            given(productVectorSearchPort.search(any(float[].class), anyInt())).willReturn(List.of());

            // when
            List<RankedProductDto> result = hybridSearchService.search("아무거나", 2);

            // then
            assertThat(result).hasSize(2);
        }
    }
}
