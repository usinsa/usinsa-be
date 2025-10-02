package com.usinsa.backend.standard.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    /**
     * JWT 토큰 생성
     *
     * @param secretKey     서명에 사용할 **비밀 키**
     * @param expireSeconds 토큰 만료 시간(초)
     * @param claims        토큰에 포함할 클레임(=payload)(예: 사용자 정보)
     * @return 생성된 JWT 토큰 문자열
     */
    public static String createToken(Key secretKey, int expireSeconds, Map<String, Object> claims) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + 1000L * expireSeconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
