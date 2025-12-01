package com.usinsa.backend.domain.auth.token;

import com.usinsa.backend.domain.auth.token.store.RefreshTokenStore;
import com.usinsa.backend.domain.auth.token.store.TokenBlacklist;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import com.usinsa.backend.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * JwtTokenService 단위 테스트
 * - 토큰 발급, 갱신, 로그아웃, 블랙리스트 기능 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenService 단위 테스트")
class JwtTokenServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenStore refreshStore;

    @Mock
    private TokenBlacklist blacklist;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private JwtTokenService jwtTokenService;

    private final String TEST_SECRET = "test-secret-key-for-jwt-token-signing-must-be-long-enough";
    private final long ACCESS_EXP = 1800L;  // 30분
    private final long REFRESH_EXP = 1209600L;  // 14일

    @BeforeEach
    void setUp() {
        given(jwtProperties.getSecret()).willReturn(TEST_SECRET);
        given(jwtProperties.getAccessExpireSeconds()).willReturn(ACCESS_EXP);
        given(jwtProperties.getRefreshExpireSeconds()).willReturn(REFRESH_EXP);
    }

    @Test
    @DisplayName("토큰 발급 성공 - Access Token과 Refresh Token 생성")
    void issueTokens_Success() {
        // given
        Long memberId = 1L;
        String email = "test@example.com";
        List<String> roles = List.of("ROLE_USER");
        String deviceId = "device-123";

        willDoNothing().given(refreshStore).save(any(TokenMeta.class));

        // when
        TokenPair result = jwtTokenService.issueTokens(memberId, email, roles, deviceId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getAccessExpEpochSec()).isGreaterThan(Instant.now().getEpochSecond());
        assertThat(result.getRefreshExpEpochSec()).isGreaterThan(Instant.now().getEpochSecond());

        // Access Token 검증
        Claims accessClaims = JwtUtil.parse(TEST_SECRET, result.getAccessToken());
        assertThat(accessClaims.get("uid", Long.class)).isEqualTo(memberId);
        assertThat(accessClaims.get("rol", List.class)).isEqualTo(roles);
        assertThat(accessClaims.get("typ", String.class)).isEqualTo(TokenType.ACCESS.getValue());
        assertThat(accessClaims.get("jti", String.class)).isNotBlank();

        // Refresh Token 검증
        Claims refreshClaims = JwtUtil.parse(TEST_SECRET, result.getRefreshToken());
        assertThat(refreshClaims.get("uid", Long.class)).isEqualTo(memberId);
        assertThat(refreshClaims.get("dev", String.class)).isEqualTo(deviceId);
        assertThat(refreshClaims.get("typ", String.class)).isEqualTo(TokenType.REFRESH.getValue());
        assertThat(refreshClaims.get("jti", String.class)).isNotBlank();

        // Redis 저장 검증
        verify(refreshStore).save(argThat(meta ->
                meta.getMemberId().equals(memberId) &&
                meta.getEmail().equals(email) &&
                meta.getDeviceId().equals(deviceId) &&
                meta.getRoles().equals(roles)
        ));
    }

    @Test
    @DisplayName("토큰 갱신 성공 - Refresh Token Rotation")
    void rotateTokens_Success() {
        // given
        Long memberId = 1L;
        String deviceId = "device-123";
        
        // 기존 Refresh Token 생성
        TokenPair oldTokens = jwtTokenService.issueTokens(
                memberId, "test@example.com", List.of("ROLE_USER"), deviceId
        );
        
        // Redis에 저장된 메타정보 Mock
        Claims oldRefreshClaims = JwtUtil.parse(TEST_SECRET, oldTokens.getRefreshToken());
        String oldJti = oldRefreshClaims.get("jti", String.class);
        
        TokenMeta storedMeta = TokenMeta.builder()
                .jti(oldJti)
                .memberId(memberId)
                .email("test@example.com")
                .roles(List.of("ROLE_USER"))
                .deviceId(deviceId)
                .expiresAt(Instant.now().plusSeconds(REFRESH_EXP))
                .build();
        
        given(refreshStore.find(memberId, deviceId))
                .willReturn(Optional.of(storedMeta));
        willDoNothing().given(refreshStore).save(any(TokenMeta.class));

        // when
        TokenPair newTokens = jwtTokenService.rotateTokens(oldTokens.getRefreshToken(), deviceId);

        // then
        assertThat(newTokens).isNotNull();
        assertThat(newTokens.getAccessToken()).isNotEqualTo(oldTokens.getAccessToken());
        assertThat(newTokens.getRefreshToken()).isNotEqualTo(oldTokens.getRefreshToken());

        // 새 토큰의 JTI가 변경되었는지 확인
        Claims newRefreshClaims = JwtUtil.parse(TEST_SECRET, newTokens.getRefreshToken());
        String newJti = newRefreshClaims.get("jti", String.class);
        assertThat(newJti).isNotEqualTo(oldJti);

        verify(refreshStore).find(memberId, deviceId);
        verify(refreshStore).save(any(TokenMeta.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 재사용된 Refresh Token (Replay Attack)")
    void rotateTokens_Fail_TokenReused() {
        // given
        Long memberId = 1L;
        String deviceId = "device-123";
        
        // 탈취된 구 Refresh Token
        TokenPair oldTokens = jwtTokenService.issueTokens(
                memberId, "test@example.com", List.of("ROLE_USER"), deviceId
        );
        
        // Redis에는 새 JTI가 저장되어 있음 (이미 한 번 갱신됨)
        TokenMeta storedMeta = TokenMeta.builder()
                .jti("new-jti-after-rotation")  // 다른 JTI!
                .memberId(memberId)
                .email("test@example.com")
                .roles(List.of("ROLE_USER"))
                .deviceId(deviceId)
                .expiresAt(Instant.now().plusSeconds(REFRESH_EXP))
                .build();
        
        given(refreshStore.find(memberId, deviceId))
                .willReturn(Optional.of(storedMeta));

        // when & then
        assertThatThrownBy(() -> jwtTokenService.rotateTokens(oldTokens.getRefreshToken(), deviceId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_REUSED);

        // 재사용 공격 탐지 시 Redis 데이터 삭제 확인
        verify(refreshStore).find(memberId, deviceId);
        verify(refreshStore).delete(memberId, deviceId);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 폐기된 Refresh Token")
    void rotateTokens_Fail_TokenRevoked() {
        // given
        TokenPair tokens = jwtTokenService.issueTokens(
                1L, "test@example.com", List.of("ROLE_USER"), "device-123"
        );
        
        // Redis에 저장된 메타정보 없음 (로그아웃 등으로 삭제됨)
        given(refreshStore.find(anyLong(), anyString()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> jwtTokenService.rotateTokens(tokens.getRefreshToken(), "device-123"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_REVOKED);

        verify(refreshStore).find(anyLong(), anyString());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 잘못된 토큰 타입 (Access Token 사용)")
    void rotateTokens_Fail_WrongTokenType() {
        // given
        TokenPair tokens = jwtTokenService.issueTokens(
                1L, "test@example.com", List.of("ROLE_USER"), "device-123"
        );
        
        String accessToken = tokens.getAccessToken();  // Access Token을 Refresh 용도로 사용 시도

        // when & then
        assertThatThrownBy(() -> jwtTokenService.rotateTokens(accessToken, "device-123"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_TYPE_MISMATCH);
    }

    @Test
    @DisplayName("로그아웃 성공 - Access Token 블랙리스트 등록")
    void logout_Success() {
        // given
        TokenPair tokens = jwtTokenService.issueTokens(
                1L, "test@example.com", List.of("ROLE_USER"), "device-123"
        );
        
        String accessToken = tokens.getAccessToken();
        willDoNothing().given(blacklist).blacklist(anyString(), any(Instant.class));

        // when
        jwtTokenService.logout(accessToken);

        // then
        Claims claims = JwtUtil.parse(TEST_SECRET, accessToken);
        String jti = claims.get("jti", String.class);
        Instant exp = claims.getExpiration().toInstant();
        
        verify(blacklist).blacklist(jti, exp);
    }

    @Test
    @DisplayName("블랙리스트 확인 - 로그아웃된 토큰")
    void isBlacklisted_True() {
        // given
        TokenPair tokens = jwtTokenService.issueTokens(
                1L, "test@example.com", List.of("ROLE_USER"), "device-123"
        );
        
        String accessToken = tokens.getAccessToken();
        given(blacklist.isBlacklisted(anyString())).willReturn(true);

        // when
        boolean result = jwtTokenService.isBlacklisted(accessToken);

        // then
        assertThat(result).isTrue();
        verify(blacklist).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("블랙리스트 확인 - 정상 토큰")
    void isBlacklisted_False() {
        // given
        TokenPair tokens = jwtTokenService.issueTokens(
                1L, "test@example.com", List.of("ROLE_USER"), "device-123"
        );
        
        String accessToken = tokens.getAccessToken();
        given(blacklist.isBlacklisted(anyString())).willReturn(false);

        // when
        boolean result = jwtTokenService.isBlacklisted(accessToken);

        // then
        assertThat(result).isFalse();
        verify(blacklist).isBlacklisted(anyString());
    }

    @Test
    @DisplayName("Access Token 추출 성공 - Authorization 헤더")
    void resolveAccessToken_Success() {
        // given
        String token = "test.jwt.token";
        given(request.getHeader(HttpHeaders.AUTHORIZATION))
                .willReturn("Bearer " + token);

        // when
        String result = jwtTokenService.resolveAccessToken(request);

        // then
        assertThat(result).isEqualTo(token);
        verify(request).getHeader(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("Access Token 추출 실패 - Authorization 헤더 없음")
    void resolveAccessToken_NoHeader() {
        // given
        given(request.getHeader(HttpHeaders.AUTHORIZATION))
                .willReturn(null);

        // when
        String result = jwtTokenService.resolveAccessToken(request);

        // then
        assertThat(result).isNull();
        verify(request).getHeader(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("Access Token 추출 실패 - Bearer 스킴 없음")
    void resolveAccessToken_NoBearerScheme() {
        // given
        given(request.getHeader(HttpHeaders.AUTHORIZATION))
                .willReturn("test.jwt.token");  // Bearer 없음

        // when
        String result = jwtTokenService.resolveAccessToken(request);

        // then
        assertThat(result).isNull();
        verify(request).getHeader(HttpHeaders.AUTHORIZATION);
    }

    @Test
    @DisplayName("Device ID 추출 - X-Device-Id 헤더")
    void resolveDeviceId_FromHeader() {
        // given
        given(request.getHeader("X-Device-Id"))
                .willReturn("custom-device-123");

        // when
        String result = jwtTokenService.resolveDeviceId(request);

        // then
        assertThat(result).isEqualTo("custom-device-123");
        verify(request).getHeader("X-Device-Id");
    }

    @Test
    @DisplayName("Device ID 추출 - User-Agent 해시값")
    void resolveDeviceId_FromUserAgent() {
        // given
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        given(request.getHeader("X-Device-Id")).willReturn(null);
        given(request.getHeader("User-Agent")).willReturn(userAgent);

        // when
        String result = jwtTokenService.resolveDeviceId(request);

        // then
        assertThat(result).isNotBlank();
        assertThat(result).isEqualTo(Integer.toHexString(userAgent.hashCode()));
        verify(request).getHeader("X-Device-Id");
        verify(request).getHeader("User-Agent");
    }
}
