package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("prod")
public class ZincSearchClient {

    private final ZincSearchProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ZincSearchClient(ZincSearchProperties props,
                            ObjectMapper objectMapper,
                            @Qualifier("zincRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    // ── 공통 헤더 ─────────────────────────────────────────────────────

    HttpHeaders headers() {
        String creds = props.getUsername() + ":" + props.getPassword();
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return h;
    }

    // ── 인덱스 생성 ───────────────────────────────────────────────────

    public void createIndexIfNotExists() {
        String url = props.getUrl() + "/api/index";

        Map<String, Object> body = Map.of(
                "name", props.getIndex(),
                "storage_type", "disk",
                "settings", Map.of(
                        "analysis", Map.of(
                                "analyzer", Map.of(
                                        //ngram 모든 부분 문자열 생성 (한글 포함)
                                        "korean_ngram_analyzer", Map.of(
                                                "type", "custom",
                                                "tokenizer", "korean_ngram_tokenizer",
                                                "filter", List.of("lowercase")
                                        )
                                ),
                                "tokenizer", Map.of(
                                        "korean_ngram_tokenizer", Map.of(
                                                "type", "ngram",
                                                "min_gram", 1,
                                                "max_gram", 10,
                                                // token_chars 빈 배열 = 공백 외 모든 문자(한글 포함) 토큰화
                                                "token_chars", List.of()
                                        )
                                )
                        )
                ),
                "mappings", Map.of(
                        "properties", Map.of(
                                // 색인: ngram 분석기(한글 부분 매칭)
                                // 검색: standard(입력 키워드를 그대로 사용)
                                "name", Map.of(
                                        "type", "text",
                                        "analyzer", "korean_ngram_analyzer",
                                        "search_analyzer", "standard"
                                ),
                                "brandName", Map.of(
                                        "type", "text",
                                        "analyzer", "korean_ngram_analyzer",
                                        "search_analyzer", "standard"
                                ),
                                "categoryName", Map.of(
                                        "type", "text",
                                        "analyzer", "korean_ngram_analyzer",
                                        "search_analyzer", "standard"
                                ),
                                "price", Map.of("type", "long"),
                                "likeCount", Map.of("type", "integer"),
                                "clickCount", Map.of("type", "integer")
                        )
                )
        );

        try {
            String json = objectMapper.writeValueAsString(body);
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(json, headers()), String.class);
            log.info("ZincSearch index 생성 완료");
        } catch (Exception e) {
            log.warn("Index 이미 존재하거나 생성 실패: {}", e.getMessage());
        }
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
                        "bool", Map.of(
                                "should", List.of(
                                        // 완전 문구 매칭 (가장 높은 우선순위)
                                        Map.of("multi_match", Map.of(
                                                "query", keyword,
                                                "fields", List.of("name^4", "brandName^2", "categoryName"),
                                                "type", "phrase"
                                        )),
                                        // 개별 토큰 매칭 (부분 매칭)
                                        Map.of("multi_match", Map.of(
                                                "query", keyword,
                                                "fields", List.of("name^3", "brandName^2", "categoryName"),
                                                "type", "best_fields",
                                                "operator", "or"
                                        ))
                                ),
                                "minimum_should_match", 1
                        )
                ),
                "size", 50,
                "sort", List.of(
                        Map.of("_score", Map.of("order", "desc"))
                )
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

    // ── 인덱스 삭제 ───────────────────────────────────────────────────

    public void deleteIndex() {
        String url = props.getUrl() + "/api/index/" + props.getIndex();
        try {
            restTemplate.exchange(url, HttpMethod.DELETE,
                    new HttpEntity<>(headers()), String.class);
            log.info("ZincSearch index 삭제 완료: {}", props.getIndex());
        } catch (Exception e) {
            log.warn("ZincSearch index 삭제 실패: {}", e.getMessage());
        }
    }
}
