package com.usinsa.backend.domain.search.history.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchHistoryRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String HISTORY_KEY_PREFIX = "search:history:"; // 예: search:history:123

    // 사용자별 검색어 기록 추가
    public void addUserSearchHistory(Long userId, String keyword) {
        String key = HISTORY_KEY_PREFIX + userId;
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, 9); // 최근 10개만 유지
    }

    // 사용자별 최근 검색어 목록 조회
    public List<String> getUserSearchHistory(Long userId) {
        String key = HISTORY_KEY_PREFIX + userId;
        return redisTemplate.opsForList().range(key, 0, 9);
    }
}