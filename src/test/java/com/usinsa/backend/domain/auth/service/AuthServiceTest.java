package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 단위 테스트
 * - 로그인, 토큰 갱신, 로그아웃 기능 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private Member testMember;
    private TokenPair testTokenPair;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 데이터
        testMember = Member.builder()
                .id(1L)
                .usinaId("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword") // BCrypt 인코딩된 비밀번호
                .name("테스트유저")
                .nickname("테스터")
                .phone("01012345678")
                .isAdmin(false)
                .build();

        // 테스트용 토큰 쌍
        testTokenPair = TokenPair.builder()
                .accessToken("test.access.token")
                .refreshToken("test.refresh.token")
                .accessExpEpochSec(System.currentTimeMillis() / 1000 + 1800)
                .refreshExpEpochSec(System.currentTimeMillis() / 1000 + 1209600)
                .build();
    }

    @Test
    @DisplayName("로그인 성공 - 올바른 이메일과 비밀번호")
    void login_Success() {
        // given
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("password123");

        given(memberRepository.findByEmail(anyString()))
                .willReturn(Optional.of(testMember));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(true);
        given(tokenService.resolveDeviceId(any()))
                .willReturn("device-123");
        given(tokenService.issueTokens(anyLong(), anyString(), anyList(), anyString()))
                .willReturn(testTokenPair);

        // when
        AuthDto.LoginRes result = authService.login(loginReq, request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("테스트유저");
        assertThat(result.getNickname()).isEqualTo("테스터");
        assertThat(result.getAccessToken()).isEqualTo("test.access.token");
        assertThat(result.getRefreshToken()).isEqualTo("test.refresh.token");

        // verify
        verify(memberRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", testMember.getPassword());
        verify(tokenService).resolveDeviceId(request);
        verify(tokenService).issueTokens(
                eq(1L),
                eq("test@example.com"),
                any(List.class),
                eq("device-123")
        );
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_MemberNotFound() {
        // given
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail("notfound@example.com");
        loginReq.setPassword("password123");

        given(memberRepository.findByEmail(anyString()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginReq, request, response))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());

        verify(memberRepository).findByEmail("notfound@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).issueTokens(anyLong(), anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        AuthDto.LoginReq loginReq = new AuthDto.LoginReq();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("wrongpassword");

        given(memberRepository.findByEmail(anyString()))
                .willReturn(Optional.of(testMember));
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(loginReq, request, response))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 일치하지 않습니다");

        verify(memberRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", testMember.getPassword());
        verify(tokenService, never()).issueTokens(anyLong(), anyString(), anyList(), anyString());
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_Success() {
        // given
        AuthDto.RefreshReq refreshReq = new AuthDto.RefreshReq();
        refreshReq.setRefreshToken("old.refresh.token");

        given(tokenService.resolveDeviceId(any()))
                .willReturn("device-123");
        given(tokenService.rotateTokens(anyString(), anyString()))
                .willReturn(testTokenPair);

        // when
        TokenPair result = authService.refresh(refreshReq, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("test.access.token");
        assertThat(result.getRefreshToken()).isEqualTo("test.refresh.token");

        verify(tokenService).resolveDeviceId(request);
        verify(tokenService).rotateTokens("old.refresh.token", "device-123");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        given(tokenService.resolveAccessToken(any()))
                .willReturn("test.access.token");
        willDoNothing().given(tokenService).logout(anyString());

        // when
        authService.logout(request, response);

        // then
        verify(tokenService).resolveAccessToken(request);
        verify(tokenService).logout("test.access.token");
    }

    @Test
    @DisplayName("로그아웃 - 토큰이 없는 경우")
    void logout_NoToken() {
        // given
        given(tokenService.resolveAccessToken(any()))
                .willReturn(null);

        // when
        authService.logout(request, response);

        // then
        verify(tokenService).resolveAccessToken(request);
        verify(tokenService, never()).logout(anyString());
    }
}
