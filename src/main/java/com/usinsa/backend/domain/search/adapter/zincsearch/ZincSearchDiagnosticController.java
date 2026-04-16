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

/**
 * ZincSearch 디버깅 전용 컨트롤러 (prod 전용, 진단 후 제거 권장)
 *
 * 실행 순서:
 * 1. GET /zinc-debug/step1-index              → 인덱스 존재 확인
 * 2. GET /zinc-debug/step2-docs               → 색인 문서 확인
 * 3. GET /zinc-debug/step4-match?q=나이키      → match 쿼리 테스트
 * 4. GET /zinc-debug/step5-querystring?q=나이키 → query_string 테스트
 * 5. GET /zinc-debug/step6-compare?q=나이키    → 3가지 쿼리 결과 비교
 */
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

    // Step 1 & 3: 인덱스 존재 + 매핑 확인
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

    // Step 4: 최소 match 쿼리
    @GetMapping("/step4-match")
    public ResponseEntity<String> step4Match(@RequestParam(defaultValue = "나이키") String q) {
        return rawPost("/api/" + props.getIndex() + "/_search",
                "{\"query\":{\"match\":{\"name\":\"" + q + "\"}},\"size\":5}");
    }

    // Step 5: query_string wildcard (현재 코드 방식)
    @GetMapping("/step5-querystring")
    public ResponseEntity<String> step5QueryString(@RequestParam(defaultValue = "나이키") String q) {
        String expr = "name:*" + q + "* OR brandName:*" + q + "* OR categoryName:*" + q + "*";
        return rawPost("/api/" + props.getIndex() + "/_search",
                "{\"query\":{\"query_string\":{\"query\":\"" + expr + "\"}},\"size\":5}");
    }

    // Step 6: 3가지 쿼리 hit count 비교
    @GetMapping("/step6-compare")
    public ResponseEntity<Map<String, Object>> step6Compare(@RequestParam(defaultValue = "나이키") String q) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("1_match",
                hitCount(rawPost("/api/" + props.getIndex() + "/_search",
                        "{\"query\":{\"match\":{\"name\":\"" + q + "\"}},\"size\":5}")));

        result.put("2_multi_match",
                hitCount(rawPost("/api/" + props.getIndex() + "/_search",
                        "{\"query\":{\"multi_match\":{\"query\":\"" + q + "\",\"fields\":[\"name\",\"brandName\",\"categoryName\"]}},\"size\":5}")));

        String expr = "name:*" + q + "* OR brandName:*" + q + "* OR categoryName:*" + q + "*";
        result.put("3_query_string_wildcard",
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
