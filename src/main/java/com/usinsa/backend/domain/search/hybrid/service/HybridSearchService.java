package com.usinsa.backend.domain.search.hybrid.service;

import com.usinsa.backend.domain.product.entity.Product;
import com.usinsa.backend.domain.product.repository.ProductRepository;
import com.usinsa.backend.domain.search.embedding.client.EmbeddingClient;
import com.usinsa.backend.domain.search.hybrid.dto.RankedProductDto;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.port.ProductVectorSearchPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import com.usinsa.backend.domain.search.vector.dto.VectorSearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProductSearchPort(키워드) + ProductVectorSearchPort(벡터) 결과를
 * RRF(Reciprocal Rank Fusion)로 병합한다.
 * 두 Port는 서로를 모르며, 병합 책임은 오직 이 서비스에만 있다 (1단계 원칙).
 */
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int RRF_K = 60;

    private final ProductSearchPort productSearchPort;
    private final ProductVectorSearchPort productVectorSearchPort;
    private final EmbeddingClient embeddingClient;
    private final ProductRepository productRepository;

    public List<RankedProductDto> search(String keyword, int topK) {
        List<ProductSearchDto> keywordResults = productSearchPort.search(keyword);

        float[] queryVector = embeddingClient.embedQuery(keyword);
        List<VectorSearchResultDto> vectorResults = productVectorSearchPort.search(queryVector, topK);

        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, ProductSearchDto> keywordInfoById = new HashMap<>();

        for (int i = 0; i < keywordResults.size(); i++) {
            ProductSearchDto dto = keywordResults.get(i);
            int rank = i + 1; // 1-based rank
            rrfScores.merge(dto.getId(), rrfScore(rank), Double::sum);
            keywordInfoById.put(dto.getId(), dto);
        }

        for (int i = 0; i < vectorResults.size(); i++) {
            VectorSearchResultDto dto = vectorResults.get(i);
            int rank = i + 1;
            rrfScores.merge(dto.getProductId(), rrfScore(rank), Double::sum);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> toRankedProductDto(entry.getKey(), entry.getValue(), keywordInfoById))
                .toList();
    }

    private double rrfScore(int rank) {
        return 1.0 / (RRF_K + rank);
    }

    /**
     * 키워드 검색에 이미 있던 상품은 ProductSearchDto에서 바로 정보를 가져오고,
     * 벡터 검색에서만 발견된 상품(키워드 검색엔 안 걸림)은 ProductRepository로 직접 조회해서 채운다.
     */
    private RankedProductDto toRankedProductDto(Long productId, double rrfScore,
                                                 Map<Long, ProductSearchDto> keywordInfoById) {
        ProductSearchDto info = keywordInfoById.get(productId);
        if (info != null) {
            return RankedProductDto.builder()
                    .productId(productId)
                    .name(info.getName())
                    .brandName(info.getBrandName())
                    .categoryName(info.getCategoryName())
                    .price(info.getPrice())
                    .rrfScore(rrfScore)
                    .build();
        }

        return productRepository.findById(productId)
                .map(p -> RankedProductDto.builder()
                        .productId(productId)
                        .name(p.getName())
                        .brandName(p.getBrandName())
                        .categoryName(p.getCategory().getName())
                        .price(p.getPrice())
                        .rrfScore(rrfScore)
                        .build())
                .orElseGet(() -> RankedProductDto.builder()
                        .productId(productId)
                        .rrfScore(rrfScore)
                        .build());
    }
}
