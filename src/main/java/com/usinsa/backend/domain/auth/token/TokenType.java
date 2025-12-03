package com.usinsa.backend.domain.auth.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * JWT 토큰 타입
 */
@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;
}
