package com.usinsa.backend.domain.search.trend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SearchTrendRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String TREND_KEY = "search:trending";

    // 검색어 점수(횟수) 증가
    public void incrementKeyword(String keyword) {
        redisTemplate.opsForZSet().incrementScore(TREND_KEY, keyword, 1);
    }

    // 인기 검색어 상위 N개 조회
    public Set<String> getTopKeywords(int limit) {
        return redisTemplate.opsForZSet().reverseRange(TREND_KEY, 0, limit - 1);
    }
}