package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.usinsa.backend.domain.search.port.ProductIndexPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ZincSearchIndexAdapter implements ProductIndexPort {

    private final ZincSearchClient client;

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

        // ZincSearch Bulk API: NDJSON 포맷
        // { "index": { "_index": "products", "_id": "1" } }
        // { "id": 1, "name": "...", ... }
        StringBuilder sb = new StringBuilder();
        for (ProductSearchDto dto : docs) {
            sb.append("{\"index\":{\"_index\":\"products\",\"_id\":\"").append(dto.getId()).append("\"}}\n");
            sb.append(toNdjsonLine(dto)).append("\n");
        }
        client.bulkIndex(sb.toString());
    }

    @Override
    public long count() {
        return client.count();
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
        return "{\"id\":" + dto.getId()
                + ",\"name\":\"" + dto.getName() + "\""
                + ",\"brandName\":\"" + dto.getBrandName() + "\""
                + ",\"categoryName\":\"" + dto.getCategoryName() + "\""
                + ",\"price\":" + dto.getPrice()
                + ",\"likeCount\":" + dto.getLikeCount()
                + ",\"clickCount\":" + dto.getClickCount()
                + "}";
    }
}
