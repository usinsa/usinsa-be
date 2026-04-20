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

        for (ProductSearchDto dto : docs) {
            client.index(String.valueOf(dto.getId()), toMap(dto));
        }
    }
    @Override
    public long count() {
        return client.count();
    }

    @Override
    public void initIndex() {
        client.deleteIndex();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        client.createIndexIfNotExists();
        client.waitForIndexReady();
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
}
