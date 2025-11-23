package com.usinsa.backend.domain.search.history.service;

import com.usinsa.backend.domain.search.history.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository historyRepository;

    public void saveUserSearch(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank()) {
            return;
        }
        historyRepository.addUserSearchHistory(userId, keyword.trim());
    }

    public List<String> getRecentSearches(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return historyRepository.getUserSearchHistory(userId);
    }
}