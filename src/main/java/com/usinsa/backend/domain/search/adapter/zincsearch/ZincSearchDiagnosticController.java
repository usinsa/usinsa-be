package com.usinsa.backend.domain.search.adapter.zincsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/zinc-debug")
@Profile("prod")
public class ZincSearchDiagnosticController {

    private final ZincSearchProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ZincSearchDiagnosticController(ZincSearchProperties props,
                                          ObjectMapper objectMapper,
                                          @Qualifier("zincRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    // Step 1: 인덱스 존재 + 매핑 확인 (gse 분석기가 프로퍼티에 잘 들어갔는지 JSON 눈으로 확인 가능)
    @GetMapping("/step1-index")
    public ResponseEntity<String> step1Index() {
        return rawGet("/api/index/" + props.getIndex());
    }

    // Step 2: 문서 존재 확인
    @GetMapping("/step2-docs")
    public ResponseEntity<String> step2Docs() {
        return rawPost("/api/" + props.getIndex() + "/_search",
                "{\"query\":{\"match_all\":{}},\"size\":3}");
    }

    // Step 4: 단일 필드 match 쿼리 테스트 (gse 분석기로 쪼개진 단어가 정상 매칭되는지 확인)
    @GetMapping("/step4-match")
    public ResponseEntity<String> step4Match(@RequestParam(defaultValue = "나이키") String q) {
        return rawPost("/api/" + props.getIndex() + "/_search",
                "{\"query\":{\"match\":{\"name\":\"" + q + "\"}},\"size\":5}");
    }

    // Step 5: 다중 필드 multi_match 테스트 (실제 서비스에서 유용하게 쓸 수 있는 고도화 쿼리)
    @GetMapping("/step5-multimatch")
    public ResponseEntity<String> step5MultiMatch(@RequestParam(defaultValue = "나이키") String q) {
        return rawPost("/api/" + props.getIndex() + "/_search",
                "{\"query\":{\"multi_match\":{\"query\":\"" + q + "\",\"fields\":[\"name\",\"brandName\",\"categoryName\"]}},\"size\":5}");
    }

    // Step 6: 분석기 도입 후 쿼리별 성능/결과 hit count 비교 진단
    @GetMapping("/step6-compare")
    public ResponseEntity<Map<String, Object>> step6Compare(@RequestParam(defaultValue = "나이키") String q) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 단일 상품명 match
        result.put("1_match_name_only",
                hitCount(rawPost("/api/" + props.getIndex() + "/_search",
                        "{\"query\":{\"match\":{\"name\":\"" + q + "\"}},\"size\":5}")));

        // 2. 통합 다중 필드 match (추천 방식)
        result.put("2_multi_match_fields",
                hitCount(rawPost("/api/" + props.getIndex() + "/_search",
                        "{\"query\":{\"multi_match\":{\"query\":\"" + q + "\",\"fields\":[\"name\",\"brandName\",\"categoryName\"]}},\"size\":5}")));

        // 3. 기존의 와일드카드 방식 (비교용으로 유지하되 gse와의 차이점 식별용)
        String expr = "name:*" + q + "* OR brandName:*" + q + "* OR categoryName:*" + q + "*";
        result.put("3_legacy_query_string_wildcard",
                hitCount(rawPost("/api/" + props.getIndex() + "/_search",
                        "{\"query\":{\"query_string\":{\"query\":\"" + expr + "\"}},\"size\":5}")));

        return ResponseEntity.ok(result);
    }

    // ── helpers ───────────────────────────────────────────────────────

    private ResponseEntity<String> rawGet(String path) {
        try {
            return restTemplate.exchange(props.getUrl() + path,
                    HttpMethod.GET, new HttpEntity<>(headers()), String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    private ResponseEntity<String> rawPost(String path, String body) {
        try {
            return restTemplate.exchange(props.getUrl() + path,
                    HttpMethod.POST, new HttpEntity<>(body, headers()), String.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ERROR: " + e.getMessage());
        }
    }

    private int hitCount(ResponseEntity<String> res) {
        try {
            JsonNode node = objectMapper.readTree(res.getBody());
            return node.path("hits").path("total").path("value").asInt(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    private HttpHeaders headers() {
        String encoded = Base64.getEncoder()
                .encodeToString((props.getUsername() + ":" + props.getPassword()).getBytes());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        return h;
    }
}