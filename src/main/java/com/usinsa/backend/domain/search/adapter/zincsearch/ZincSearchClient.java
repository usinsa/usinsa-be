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

    // ── 인덱스 생성 (GSE 분석기 적용 수정) ─────────────────────────────────
    public void createIndexIfNotExists() {
        String url = props.getUrl() + "/api/index";

        Map<String, Object> body = Map.of(
                "name", props.getIndex(),
                "storage_type", "disk",
                "mappings", Map.of(
                        "properties", Map.of(
                                "name", Map.of(
                                        "type", "text",
                                        "analyzer", "cjk",          // 수정: gse -> cjk
                                        "search_analyzer", "cjk"   // 수정: gse -> cjk
                                ),
                                "brandName", Map.of(
                                        "type", "text",
                                        "analyzer", "cjk",          // 수정: gse -> cjk
                                        "search_analyzer", "cjk"
                                ),
                                "categoryName", Map.of(
                                        "type", "text",
                                        "analyzer", "cjk",          // 수정: gse -> cjk
                                        "search_analyzer", "cjk"
                                ),
                                "price", Map.of("type", "long"),
                                "likeCount", Map.of("type", "integer"),
                                "clickCount", Map.of("type", "integer")
                        )
                )
        );

        try {
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(json, headers()), String.class
            );

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("인덱스 생성 실패: " + res.getBody());
            }

            log.info("ZincSearch index 생성 완료 (cjk 분석기 적용)");
        } catch (Exception e) {
            throw new RuntimeException("ZincSearch index 생성 실패", e);
        }
    }

    // ── 인덱스 준비 대기 ──────────────────────────────────────────────
    public void waitForIndexReady() {
        String url = props.getUrl() + "/api/index/" + props.getIndex();
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(500);
                ResponseEntity<String> res = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
                if (res.getStatusCode().is2xxSuccessful()) {
                    log.info("ZincSearch index 준비 완료 ({}회 시도)", i + 1);
                    return;
                }
            } catch (Exception ignored) {}
            log.info("ZincSearch index 준비 대기 중... ({}/10)", i + 1);
        }
        log.warn("ZincSearch index 준비 확인 실패 - 색인을 계속 진행합니다");
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

    // ── 키워드 검색 (match_phrase -> match 쿼리로 수정) ─────────────────
    public JsonNode search(String keyword) {
        String url = props.getUrl() + "/api/" + props.getIndex() + "/_search";

        // 수정: 지나치게 엄격한 match_phrase 대신 일반 match를 사용하여 형태소 분석 효율을 극대화합니다.
        Map<String, Object> query = Map.of(
                "query", Map.of(
                        "bool", Map.of(
                                "must", List.of(
                                        Map.of(
                                                "match", Map.of("name", keyword)
                                        )
                                )
                        )
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

    // ── 인덱스 삭제 ───────────────────────────────────────────────────
    public void deleteIndex() {
        String url = props.getUrl() + "/api/index/" + props.getIndex();
        try {
            ResponseEntity<String> res = restTemplate.exchange(
                    url, HttpMethod.DELETE,
                    new HttpEntity<>(headers()), String.class
            );

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("인덱스 삭제 실패");
            }
            log.info("ZincSearch index 삭제 완료: {}", props.getIndex());

        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            if (e.getResponseBodyAsString().contains("does not exists")) {
                log.info("삭제하려는 인덱스({})가 이미 존재하지 않습니다. 인덱스 생성을 계속 진행합니다.", props.getIndex());
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("인덱스 삭제 중 알 수 없는 오류 발생", e);
        }
    }
}