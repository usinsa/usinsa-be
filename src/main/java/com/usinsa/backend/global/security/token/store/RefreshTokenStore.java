package com.usinsa.backend.global.security.token.store;

public interface RefreshTokenStore {
    void save(TokenMeta meta);                      // 최신 리프레시 jti 저장
    Optional<TokenMeta> find(Long memberId, String deviceId);
    void delete(Long memberId, String deviceId);
}