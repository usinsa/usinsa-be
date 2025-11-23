package com.usinsa.backend.domain.search.trend.service;

import com.usinsa.backend.domain.search.trend.repository.SearchTrendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchTrendService {

    private final SearchTrendRepository trendRepository;

    public void recordKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        trendRepository.incrementKeyword(keyword.trim());
    }

    public Set<String> getTrendingKeywords(int limit) {
        if (limit <= 0) {
            return Collections.emptySet();
        }
        return trendRepository.getTopKeywords(limit);
    }

}