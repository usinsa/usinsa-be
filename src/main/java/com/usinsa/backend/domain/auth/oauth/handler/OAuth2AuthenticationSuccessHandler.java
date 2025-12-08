package com.usinsa.backend.domain.auth.oauth.handler;

import com.usinsa.backend.domain.auth.oauth.service.PrincipalDetails;
import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenPair;
import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Member member = principalDetails.getMember();

        TokenPair tokenPair = jwtTokenService.generateTokenPair(member);
        String targetUrl = UriComponentsBuilder.fromUriString(getRedirectUrl())
                .queryParam("accessToken", tokenPair.getAccessToken())
                .queryParam("refreshToken", tokenPair.getRefreshToken())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getRedirectUrl() {
        // TODO: get from properties
        return "http://localhost:5173/oauth/redirect";
    }
}
