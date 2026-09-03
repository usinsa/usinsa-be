package com.usinsa.backend.domain.search.vector.adapter;

import com.usinsa.backend.domain.search.port.ProductVectorSearchPort;
import com.usinsa.backend.domain.search.vector.dto.VectorSearchResultDto;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository;
import com.usinsa.backend.domain.search.vector.repository.ProductEmbeddingRepository.NearestProductProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ProductVectorSearchPort 구현체.
 * ProductSearchPort(키워드)의 ElasticsearchSearchAdapter/ZincSearchSearchAdapter와
 * 나란히 존재하되, 서로를 참조하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PgVectorAdapter implements ProductVectorSearchPort {

    private final ProductEmbeddingRepository productEmbeddingRepository;

    @Override
    public List<VectorSearchResultDto> search(float[] queryVector, int topK) {
        String queryVectorLiteral = toVectorLiteral(queryVector);

        List<NearestProductProjection> rows =
                productEmbeddingRepository.findNearest(queryVectorLiteral, topK);

        return rows.stream()
                .map(row -> VectorSearchResultDto.builder()
                        .productId(row.getProductId())
                        .similarity(row.getSimilarity())
                        .build())
                .toList();
    }

    /**
     * float[] -> pgvector 리터럴 문자열 "[0.123,-0.456,...]"
     */
    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
