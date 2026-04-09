package com.usinsa.backend.global.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 * - Access Token 및 Refresh Token 생성
 * - 토큰 검증 및 파싱
 */
@Slf4j
public class JwtUtil {

    /**
     * JWT 토큰 생성
     *
     * @param secret        서명에 사용할 비밀 키
     * @param expireSeconds 토큰 만료 시간(초)
     * @param claims        토큰에 포함할 클레임(payload)
     * @return 생성된 JWT 토큰 문자열
     */
    public static String createToken(String secret, long expireSeconds, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireSeconds * 1000L);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰 파싱
     *
     * @param secret 서명 검증에 사용할 비밀 키
     * @param token  파싱할 JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public static Claims parse(String secret, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * JWT 토큰 유효성 검증
     *
     * @param secret 서명 검증에 사용할 비밀 키
     * @param token  검증할 JWT 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValid(String secret, String token) {
        try {
            parse(secret, token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT token invalid: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 Payload(Claims) 추출
     *
     * @param secret 서명 검증에 사용할 비밀 키
     * @param token  파싱할 JWT 토큰
     * @return Claims의 Map 형태
     */
    public static Map<String, Object> getPayload(String secret, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        return jws.getBody();
    }
}
