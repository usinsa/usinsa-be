package com.usinsa.backend.standard.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * 공용 유틸리티 클래스
 * - Json, Jwt, String 등 하위 유틸 클래스를 모아두는 용도
 */
public class Ut {

    /**
     * JWT 관련 유틸리티 클래스
     */
    public static class Jwt {

        /**
         * JWT 토큰 생성
         *
         * @param secretKey     서명에 사용할 비밀 키
         * @param expireSeconds 토큰 만료 시간(초)
         * @param claims        토큰에 포함할 클레임(=payload)
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
        /**
         * ✅ 토큰 유효성 검증
         */
        public static boolean isValidToken(String keyString, String token) {
            try {
                SecretKey secretKey = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
                Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token);
                return true; // 파싱 성공 = 유효
            } catch (JwtException | IllegalArgumentException e) {
                // SignatureException, ExpiredJwtException, MalformedJwtException 등 모두 여기로 들어옴
                return false;
            }
        }

        /**
         * ✅ 토큰 Payload(Claims) 추출
         */
        public static Map<String, Object> getPayload(String keyString, String token) {
            SecretKey secretKey = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));

            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            return jws.getBody();
        }
    }
}
