package com.usinsa.backend.domain.auth.oauth.handler;

import com.usinsa.backend.domain.auth.oauth.config.OAuth2Properties;
import com.usinsa.backend.domain.auth.oauth.service.PrincipalDetails;
import com.usinsa.backend.domain.auth.service.AuthService;
import com.usinsa.backend.domain.member.entity.Member;
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

    private final AuthService authService;
    private final OAuth2Properties oAuth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Member member = ((PrincipalDetails) authentication.getPrincipal()).getMember();
        String provider = member.getOauthProvider() != null ? member.getOauthProvider() : "unknown";

        // JWT 발급 + HttpOnly 쿠키 기록 (AuthService로 일원화)
        authService.issueAndWriteCookies(member, request, response);

        log.info("OAuth 로그인 성공: provider={}, memberId={}", provider, member.getId());

        // 쿠키가 브라우저에 세팅된 상태로 FE 콜백 페이지로 리다이렉트
        String targetUrl = oAuth2Properties.getCallbackBaseUrl() + "/" + provider;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
