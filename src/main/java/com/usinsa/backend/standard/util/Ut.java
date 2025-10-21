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
         * @param secret     서명에 사용할 비밀 키
         * @param expireSeconds 토큰 만료 시간(초)
         * @param claims        토큰에 포함할 클레임(=payload)
         * @return 생성된 JWT 토큰 문자열
         */
        public static String createToken(String secret, int expireSeconds, Map<String, Object> claims) {
            Date now = new Date();
            Date exp = new Date(now.getTime() + expireSeconds * 1000L);
            Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        }

        public static Claims parse(String secret, String token) {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        }

        /**
         * ✅ 토큰 유효성 검증
         */
        public static boolean isValid(String secret, String token) {
            try {
                parse(secret, token);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
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
