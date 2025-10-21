package com.usinsa.backend.global.filter;

import com.usinsa.backend.global.security.token.AuthTokenService;
import com.usinsa.backend.standard.util.Ut;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1) 토큰 추출 (Bearer ...)
        String token = tokenService.resolveAccessToken(request);

        // 이미 인증되어 있거나 토큰이 없으면 패스
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 2) 유효성 검증 (서명/만료)
        if (!Ut.Jwt.isValid(tokenService.getProps().getSecret(), token)) {
            chain.doFilter(request, response);
            return;
        }

        // 3) 블랙리스트 선차단
        if (tokenService.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 4) 클레임 파싱
        Claims c = Ut.Jwt.parse(tokenService.getProps().getSecret(), token);

        if (!"access".equals(c.get("typ"))) {
            chain.doFilter(request, response);
            return;
        }

        // 필수 클레임: uid(식별자), rol(권한 목록)
        Long uid = ((Number) c.get("uid")).longValue();
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) c.get("rol");

        var auth = new UsernamePasswordAuthenticationToken(
                uid,                // principal: uid (필요하면 커스텀 Principal 클래스로 교체)
                null,               // credentials
                roles.stream().map(SimpleGrantedAuthority::new).toList()
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}