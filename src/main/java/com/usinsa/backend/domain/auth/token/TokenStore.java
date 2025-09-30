package com.usinsa.backend.domain.auth.token;

import java.util.Optional;

public interface TokenStore {
    void storeRefresh(String userKey, String refreshToken, Duration ttl);
    Optional<String> getRefresh(String userKey);
    void rotateRefresh(String userKey, String newToken, Duration ttl);
    void blacklistAccess(String accessToken, Duration ttl);
    boolean isBlacklisted(String accessToken);
}

