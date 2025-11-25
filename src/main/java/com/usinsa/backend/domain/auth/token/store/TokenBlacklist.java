package com.usinsa.backend.domain.auth.token.store;

import java.time.Instant;

/**
 * Token Blacklist 인터페이스
 * 로그아웃된 Access Token을 블랙리스트에 등록하여 재사용 방지
 */
public interface TokenBlacklist {
    
    /**
     * Access Token을 블랙리스트에 등록
     *
     * @param jti       토큰 고유 ID
     * @param expiresAt 토큰 만료 시간
     */
    void blacklist(String jti, Instant expiresAt);
    
    /**
     * Access Token이 블랙리스트에 있는지 확인
     *
     * @param jti 토큰 고유 ID
     * @return 블랙리스트에 있으면 true
     */
    boolean isBlacklisted(String jti);
}
