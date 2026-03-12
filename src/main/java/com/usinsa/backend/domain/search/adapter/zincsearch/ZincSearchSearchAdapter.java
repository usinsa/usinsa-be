package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.usinsa.backend.domain.search.port.ProductSearchPort;
import com.usinsa.backend.domain.search.result.dto.ProductSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ZincSearchSearchAdapter implements ProductSearchPort {

    private final ZincSearchClient client;

    @Override
    public List<ProductSearchDto> search(String keyword) {
        JsonNode response = client.search(keyword);
        JsonNode hits = response.path("hits").path("hits");

        List<ProductSearchDto> result = new ArrayList<>();
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                JsonNode src = hit.path("_source");
                result.add(ProductSearchDto.builder()
                        .id(src.path("id").asLong())
                        .name(src.path("name").asText())
                        .brandName(src.path("brandName").asText())
                        .categoryName(src.path("categoryName").asText())
                        .price(src.path("price").asLong())
                        .likeCount(src.path("likeCount").asInt())
                        .clickCount(src.path("clickCount").asInt())
                        .build());
            }
        }
        return result;
    }
}
