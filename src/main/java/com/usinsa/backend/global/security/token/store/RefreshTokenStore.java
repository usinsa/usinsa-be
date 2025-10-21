package com.usinsa.backend.global.security.token.store;

import com.usinsa.backend.global.security.token.TokenMeta;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenStore {
    void save(TokenMeta meta);                      // 최신 리프레시 jti 저장
    Optional<TokenMeta> find(Long memberId, String deviceId);
    void delete(Long memberId, String deviceId);
}