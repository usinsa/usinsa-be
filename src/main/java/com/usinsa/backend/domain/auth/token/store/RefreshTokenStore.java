package com.usinsa.backend.domain.auth.token.store;

import com.usinsa.backend.domain.auth.token.TokenMeta;

import java.util.Optional;

/**
 * Refresh Token 저장소 인터페이스
 * Redis 등의 저장소에 Refresh Token 메타 정보를 저장/조회
 */
public interface RefreshTokenStore {
    
    /**
     * Refresh Token 메타 정보 저장
     *
     * @param meta 저장할 토큰 메타 정보
     */
    void save(TokenMeta meta);
    
    /**
     * Refresh Token 메타 정보 조회
     *
     * @param memberId 회원 ID
     * @param deviceId 디바이스 ID
     * @return 토큰 메타 정보 (없으면 empty)
     */
    Optional<TokenMeta> find(Long memberId, String deviceId);
    
    /**
     * Refresh Token 메타 정보 삭제
     *
     * @param memberId 회원 ID
     * @param deviceId 디바이스 ID
     */
    void delete(Long memberId, String deviceId);
}
