package com.usinsa.backend.global.security.token.store;

import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TokenBlacklist {
    void blacklist(String jti, Instant expiresAt);  // access jti -> 만료시각
    boolean isBlacklisted(String jti);
}
