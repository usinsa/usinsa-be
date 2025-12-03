package com.usinsa.backend.domain.auth.token;

import com.usinsa.backend.domain.auth.token.store.RefreshTokenStore;
import com.usinsa.backend.domain.auth.token.store.TokenBlacklist;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import com.usinsa.backend.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * JWT 토큰 발급, 갱신, 로그아웃 등을 담당하는 서비스
 * Stateless 원칙을 유지하며 Access/Refresh Token을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshStore;
    private final TokenBlacklist blacklist;

    /**
     * Access Token과 Refresh Token 발급
     *
     * @param memberId 회원 ID
     * @param email    회원 이메일
     * @param roles    회원 권한 목록
     * @param deviceId 디바이스 ID
     * @return TokenPair (Access Token + Refresh Token)
     */
    public TokenPair issueTokens(Long memberId, String email, List<String> roles, String deviceId) {
        Instant now = Instant.now();
        String jtiAccess = UUID.randomUUID().toString();
        String jtiRefresh = UUID.randomUUID().toString();

        // Access Token 생성
        Map<String, Object> accessClaims = new HashMap<>();
        accessClaims.put("uid", memberId);
        accessClaims.put("rol", roles);
        accessClaims.put("jti", jtiAccess);
        accessClaims.put("typ", TokenType.ACCESS.getValue());
        
        String accessToken = JwtUtil.createToken(
                jwtProperties.getSecret(),
                jwtProperties.getAccessExpireSeconds(),
                accessClaims
        );
        long accessExp = now.plusSeconds(jwtProperties.getAccessExpireSeconds()).getEpochSecond();

        // Refresh Token 생성
        Map<String, Object> refreshClaims = new HashMap<>();
        refreshClaims.put("uid", memberId);
        refreshClaims.put("jti", jtiRefresh);
        refreshClaims.put("dev", deviceId);
        refreshClaims.put("typ", TokenType.REFRESH.getValue());
        
        String refreshToken = JwtUtil.createToken(
                jwtProperties.getSecret(),
                jwtProperties.getRefreshExpireSeconds(),
                refreshClaims
        );
        Instant refreshExp = now.plusSeconds(jwtProperties.getRefreshExpireSeconds());

        // Refresh Token 메타 정보 저장 (Redis)
        refreshStore.save(TokenMeta.builder()
                .jti(jtiRefresh)
                .memberId(memberId)
                .email(email)
                .roles(roles)
                .deviceId(deviceId)
                .expiresAt(refreshExp)
                .build());

        log.info("Tokens issued for memberId={}, deviceId={}", memberId, deviceId);

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpEpochSec(accessExp)
                .refreshExpEpochSec(refreshExp.getEpochSecond())
                .build();
    }

    public TokenPair generateTokenPair(Member member) {
        List<String> roles = new ArrayList<>();
        roles.add(member.getIsAdmin() ? "ROLE_ADMIN" : "ROLE_USER");
        return issueTokens(member.getId(), member.getEmail(), roles, UUID.randomUUID().toString());
    }

    /**
     * Refresh Token을 이용한 토큰 갱신
     * Refresh Token Rotation 적용 (기존 토큰 폐기, 새 토큰 발급)
     *
     * @param refreshToken Refresh Token
     * @param deviceId     디바이스 ID
     * @return 새로운 TokenPair
     */
    public TokenPair rotateTokens(String refreshToken, String deviceId) {
        // Refresh Token 파싱 및 검증
        Claims claims;
        try {
            claims = JwtUtil.parse(jwtProperties.getSecret(), refreshToken);
        } catch (Exception e) {
            log.error("Failed to parse refresh token: {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        // 토큰 타입 검증
        if (!TokenType.REFRESH.getValue().equals(claims.get("typ"))) {
            throw new CustomException(ErrorCode.TOKEN_TYPE_MISMATCH);
        }

        Long uid = ((Number) claims.get("uid")).longValue();
        String jti = (String) claims.get("jti");

        // Redis에서 최신 Refresh Token 정보 조회
        var latestOpt = refreshStore.find(uid, deviceId);
        if (latestOpt.isEmpty()) {
            throw new CustomException(ErrorCode.TOKEN_REVOKED);
        }

        var latest = latestOpt.get();
        
        // JTI 일치 여부 확인 (재사용 공격 방지)
        if (!Objects.equals(latest.getJti(), jti)) {
            log.warn("Refresh token reused detected! memberId={}, deviceId={}", uid, deviceId);
            refreshStore.delete(uid, deviceId); // 보안을 위해 저장된 토큰 삭제
            throw new CustomException(ErrorCode.TOKEN_REUSED);
        }

        // 새로운 토큰 발급 (기존 Refresh Token은 자동으로 덮어씌워짐)
        log.info("Tokens rotated for memberId={}, deviceId={}", uid, deviceId);
        return issueTokens(uid, latest.getEmail(), latest.getRoles(), deviceId);
    }

    /**
     * 로그아웃 처리
     * Access Token을 블랙리스트에 등록하여 재사용 방지
     *
     * @param accessToken Access Token
     */
    public void logout(String accessToken) {
        try {
            Claims claims = JwtUtil.parse(jwtProperties.getSecret(), accessToken);
            
            if (!TokenType.ACCESS.getValue().equals(claims.get("typ"))) {
                log.warn("Logout attempted with non-access token");
                return;
            }

            String jti = (String) claims.get("jti");
            Instant exp = claims.getExpiration().toInstant();
            
            blacklist.blacklist(jti, exp);
            log.info("Token blacklisted: jti={}", jti);
        } catch (Exception e) {
            log.error("Failed to logout: {}", e.getMessage());
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Access Token이 블랙리스트에 있는지 확인
     *
     * @param accessToken Access Token
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String accessToken) {
        try {
            Claims claims = JwtUtil.parse(jwtProperties.getSecret(), accessToken);
            String jti = (String) claims.get("jti");
            return blacklist.isBlacklisted(jti);
        } catch (Exception e) {
            log.debug("Failed to check blacklist: {}", e.getMessage());
            return false;
        }
    }

    /**
     * HTTP 요청에서 Access Token 추출
     * Authorization 헤더의 Bearer 토큰 파싱
     *
     * @param request HttpServletRequest
     * @return Access Token (없으면 null)
     */
    public String resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * HTTP 요청에서 Device ID 추출
     * X-Device-Id 헤더 또는 User-Agent 해시값 사용
     *
     * @param request HttpServletRequest
     * @return Device ID
     */
    public String resolveDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId;
        }
        
        // Device ID가 없으면 User-Agent 해시값 사용
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
        return Integer.toHexString(Objects.hash(userAgent));
    }

    /**
     * JWT Properties 조회 (Filter에서 필요 시 사용)
     */
    public JwtProperties getProperties() {
        return jwtProperties;
    }
}
