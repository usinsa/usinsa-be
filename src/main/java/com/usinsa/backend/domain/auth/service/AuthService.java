package com.usinsa.backend.domain.auth.service;

import com.usinsa.backend.domain.auth.dto.AuthDto;
import com.usinsa.backend.domain.auth.token.JwtProperties;
import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.global.exception.CustomException;
import com.usinsa.backend.global.exception.ErrorCode;
import com.usinsa.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;

    @Value("${server.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${server.cookie.domain:}")
    private String cookieDomain;

    // ── 로그인 ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthDto.LoginRes login(AuthDto.LoginReq body,
                                  HttpServletRequest req,
                                  HttpServletResponse res) {
        Member member = memberRepository.findByEmail(body.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(body.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        TokenPair tokenPair = issueAndWriteCookies(member, req, res);
        log.info("Login: memberId={}", member.getId());
        return toLoginRes(member, tokenPair);
    }

    // ── 토큰 갱신 ─────────────────────────────────────────────────────

    public TokenPair refresh(HttpServletRequest req, HttpServletResponse res) {
        // Refresh Token을 쿠키에서 읽음
        String refreshToken = CookieUtil.getCookie(req, CookieUtil.REFRESH_TOKEN)
                .map(jakarta.servlet.http.Cookie::getValue)
                .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

        String deviceId = CookieUtil.resolveDeviceId(req);
        TokenPair tokenPair = tokenService.rotateTokens(refreshToken, deviceId);

        // 새 토큰을 쿠키에 덮어씀
        CookieUtil.addCookie(res, CookieUtil.ACCESS_TOKEN,
                tokenPair.getAccessToken(), (int) jwtProperties.getAccessExpireSeconds(), cookieSecure, cookieDomain);
        CookieUtil.addCookie(res, CookieUtil.REFRESH_TOKEN,
                tokenPair.getRefreshToken(), (int) jwtProperties.getRefreshExpireSeconds(), cookieSecure, cookieDomain);

        log.info("Token refreshed: deviceId={}", deviceId);
        return tokenPair;
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String accessToken = CookieUtil.resolveAccessToken(req);
        if (accessToken != null) {
            tokenService.logout(accessToken);
        }
        CookieUtil.clearTokenCookies(req, res);
        log.info("Logout complete");
    }

    // ── 회원가입 ──────────────────────────────────────────────────────

    @Transactional
    public void signup(AuthDto.SignupReq body) {
        if (!body.getPassword().equals(body.getPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (memberRepository.existsByEmail(body.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        memberRepository.save(Member.builder()
                .usinaId(body.getEmail())
                .email(body.getEmail())
                .password(passwordEncoder.encode(body.getPassword()))
                .name(body.getName())
                .nickname(body.getNickname())
                .phone("000-0000-0000")
                .isAdmin(false)
                .build());
    }

    /** 현재 인증된 회원 정보 조회 (쿠키 기반 인증 상태 확인) */
    @Transactional(readOnly = true)
    public AuthDto.MeRes me(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return AuthDto.MeRes.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .build();
    }

    // ── 공통 헬퍼 ─────────────────────────────────────────────────────

    /**
     * JWT 발급 후 HttpOnly 쿠키에 기록.
     * 일반 로그인과 OAuth 성공 핸들러 양쪽에서 재사용.
     */
    public TokenPair issueAndWriteCookies(Member member,
                                           HttpServletRequest req,
                                           HttpServletResponse res) {
        List<String> roles = List.of(
                Boolean.TRUE.equals(member.getIsAdmin()) ? "ROLE_ADMIN" : "ROLE_USER");
        String deviceId = CookieUtil.resolveDeviceId(req);
        TokenPair tokenPair = tokenService.issueTokens(
                member.getId(), member.getEmail(), roles, deviceId);

        CookieUtil.addCookie(res, CookieUtil.ACCESS_TOKEN,
                tokenPair.getAccessToken(), (int) jwtProperties.getAccessExpireSeconds(), cookieSecure, cookieDomain);
        CookieUtil.addCookie(res, CookieUtil.REFRESH_TOKEN,
                tokenPair.getRefreshToken(), (int) jwtProperties.getRefreshExpireSeconds(), cookieSecure, cookieDomain);
        return tokenPair;
    }

    public AuthDto.LoginRes toLoginRes(Member member, TokenPair tokenPair) {
        return AuthDto.LoginRes.builder()
                .memberId(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .accessTokenExp(tokenPair.getAccessExpEpochSec())
                .refreshTokenExp(tokenPair.getRefreshExpEpochSec())
                .build();
    }
}
