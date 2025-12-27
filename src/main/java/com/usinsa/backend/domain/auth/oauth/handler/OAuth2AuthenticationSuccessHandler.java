package com.usinsa.backend.domain.auth.oauth.handler;

import com.usinsa.backend.domain.auth.oauth.config.OAuth2Properties;
import com.usinsa.backend.domain.auth.oauth.service.PrincipalDetails;
import com.usinsa.backend.domain.auth.token.JwtProperties;
import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final OAuth2Properties oAuth2Properties;
    private final JwtProperties jwtProperties;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Member member = principalDetails.getMember();

        log.info("=== OAuth 로그인 성공 ===");
        log.info("OAuth Provider: {}", member.getOauthProvider() != null ? member.getOauthProvider() : "LOCAL");
        log.info("Member ID: {}", member.getId());
        log.info("Email: {}", member.getEmail());

        // JWT 토큰 발급
        TokenPair tokenPair = jwtTokenService.generateTokenPair(member);

        log.info("JWT 토큰 발급 완료");
        log.info("Access Token: {}...", tokenPair.getAccessToken().substring(0, Math.min(20, tokenPair.getAccessToken().length())));

        // FE로 리다이렉트 (토큰을 HttpOnly 쿠키로 전달)
        CookieUtil.addCookie(response, CookieUtil.ACCESS_TOKEN, tokenPair.getAccessToken(), (int) jwtProperties.getAccessExpireSeconds());
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN, tokenPair.getRefreshToken(), (int) jwtProperties.getRefreshExpireSeconds());

        String targetUrl = oAuth2Properties.getRedirectUrl();

        log.info("FE로 리다이렉트: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
