package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * ZincSearch REST API 공통 클라이언트
 * <p>
 * ZincSearch는 Elasticsearch 호환 API를 제공하므로
 * /_bulk, /{index}/_search, /{index}/_doc/{id} 등의 엔드포인트를 그대로 사용.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ZincSearchClient {

    private final ZincSearchProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // ── 공통 헤더 ─────────────────────────────────────────────────────

    HttpHeaders headers() {
        String creds = props.getUsername() + ":" + props.getPassword();
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return h;
    }

    // ── 단건 색인 ─────────────────────────────────────────────────────

    public void index(String id, Map<String, Object> doc) {
        String url = props.getUrl() + "/api/" + props.getIndex() + "/_doc/" + id;
        try {
            String body = objectMapper.writeValueAsString(doc);
            restTemplate.exchange(url, HttpMethod.PUT,
                    new HttpEntity<>(body, headers()), String.class);
        } catch (Exception e) {
            log.error("ZincSearch 색인 실패 id={}: {}", id, e.getMessage());
        }
    }

    // ── 단건 삭제 ─────────────────────────────────────────────────────

    public void delete(String id) {
        String url = props.getUrl() + "/api/" + props.getIndex() + "/_doc/" + id;
        try {
            restTemplate.exchange(url, HttpMethod.DELETE,
                    new HttpEntity<>(headers()), String.class);
        } catch (Exception e) {
            log.error("ZincSearch 삭제 실패 id={}: {}", id, e.getMessage());
        }
    }

    // ── 전체 건수 조회 ────────────────────────────────────────────────

    public long count() {
        String url = props.getUrl() + "/api/" + props.getIndex() + "/_count";
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            JsonNode node = objectMapper.readTree(res.getBody());
            return node.path("count").asLong(0);
        } catch (Exception e) {
            log.warn("ZincSearch 건수 조회 실패: {}", e.getMessage());
            return 0;
        }
    }

    // ── 키워드 검색 ───────────────────────────────────────────────────

    public JsonNode search(String keyword) {
        String url = props.getUrl() + "/api/" + props.getIndex() + "/_search";
        Map<String, Object> query = Map.of(
                "query", Map.of(
                        "match", Map.of(
                                "name", Map.of(
                                        "query", keyword,
                                        "operator", "or"
                                )
                        )
                ),
                "size", 50
        );
        try {
            String body = objectMapper.writeValueAsString(query);
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers()), String.class);
            return objectMapper.readTree(res.getBody());
        } catch (Exception e) {
            log.error("ZincSearch 검색 실패 keyword={}: {}", keyword, e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    // ── Bulk 색인 ─────────────────────────────────────────────────────

    public void bulkIndex(String ndjson) {
        String url = props.getUrl() + "/api/_bulk";
        try {
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(ndjson, headers()), String.class);
        } catch (Exception e) {
            log.error("ZincSearch bulk 색인 실패: {}", e.getMessage());
        }
    }
}
