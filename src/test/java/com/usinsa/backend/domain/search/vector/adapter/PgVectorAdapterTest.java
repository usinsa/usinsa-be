package com.usinsa.backend.domain.search.vector.adapter;

import com.usinsa.backend.domain.search.vector.dto.VectorSearchResultDto;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository.NearestProductProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorAdapterTest {

    @InjectMocks
    private PgVectorAdapter pgVectorAdapter;

    @Mock
    private ProductEmbeddingRepository productEmbeddingRepository;

    @Test
    @DisplayName("float[]를 pgvector 리터럴 문자열로 변환해서 repository에 전달한다")
    void search_ConvertsFloatArrayToVectorLiteral() {
        // given
        NearestProductProjection projection = new NearestProductProjection() {
            @Override public Long getProductId() { return 1L; }
            @Override public Double getSimilarity() { return 0.87; }
        };
        given(productEmbeddingRepository.findNearest(anyString(), anyInt()))
                .willReturn(List.of(projection));

        // when
        List<VectorSearchResultDto> result = pgVectorAdapter.search(new float[]{0.1f, -0.2f, 0.3f}, 5);

        // then
        ArgumentCaptor<String> literalCaptor = ArgumentCaptor.forClass(String.class);
        verify(productEmbeddingRepository).findNearest(literalCaptor.capture(), anyInt());
        assertThat(literalCaptor.getValue()).isEqualTo("[0.1,-0.2,0.3]");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(1L);
        assertThat(result.get(0).getSimilarity()).isEqualTo(0.87);
    }
}
