package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ZincSearchIndexAdapter implements ProductIndexPort {

    private final ZincSearchClient client;
    private final ObjectMapper objectMapper;
    private final ZincSearchProperties props;

    @Override
    public void save(ProductSearchDto dto) {
        client.index(String.valueOf(dto.getId()), toMap(dto));
    }

    @Override
    public void delete(Long productId) {
        client.delete(String.valueOf(productId));
    }

    @Override
    public void saveAll(List<ProductSearchDto> docs) {
        if (docs.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (ProductSearchDto dto : docs) {
            sb.append("{\"index\":{\"_index\":\"")
                    .append(props.getIndex())
                    .append("\",\"_id\":\"")
                    .append(dto.getId())
                    .append("\"}}\n");
            sb.append(toNdjsonLine(dto)).append("\n");
        }
        client.bulkIndex(sb.toString());
    }

    @Override
    public long count() {
        return client.count();
    }

    /**
     * 인덱스 초기화: 기존 인덱스를 삭제 후 재생성하여 매핑/분석기 설정을 반영
     * (설정 변경 시 기존 인덱스가 남아 있으면 새 분석기가 적용되지 않음)
     */
    @Override
    public void initIndex() {
        client.deleteIndex();
        client.createIndexIfNotExists();
    }

    // ── helpers ───────────────────────────────────────────────────────

    private Map<String, Object> toMap(ProductSearchDto dto) {
        return Map.of(
                "id",           dto.getId(),
                "name",         dto.getName(),
                "brandName",    dto.getBrandName(),
                "categoryName", dto.getCategoryName(),
                "price",        dto.getPrice(),
                "likeCount",    dto.getLikeCount(),
                "clickCount",   dto.getClickCount()
        );
    }

    private String toNdjsonLine(ProductSearchDto dto) {
        try {
            return objectMapper.writeValueAsString(toMap(dto));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
