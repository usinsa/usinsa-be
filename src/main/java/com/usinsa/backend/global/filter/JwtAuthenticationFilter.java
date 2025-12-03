package com.usinsa.backend.global.filter;

import com.usinsa.backend.domain.auth.token.JwtTokenService;
import com.usinsa.backend.domain.auth.token.TokenType;
import com.usinsa.backend.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. Authorization 헤더에서 Access Token 추출
        String token = tokenService.resolveAccessToken(request);

        // 토큰이 없거나 이미 인증된 경우 다음 필터로 진행
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 토큰 유효성 검증 (서명, 만료 시간)
        if (!JwtUtil.isValid(tokenService.getProperties().getSecret(), token)) {
            log.debug("Invalid or expired JWT token");
            chain.doFilter(request, response);
            return;
        }

        // 3. 블랙리스트 확인 (로그아웃된 토큰)
        if (tokenService.isBlacklisted(token)) {
            log.debug("Blacklisted JWT token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 4. 토큰 파싱 및 인증 정보 추출
        try {
            Claims claims = JwtUtil.parse(tokenService.getProperties().getSecret(), token);

            // Access Token 타입 확인
            if (!TokenType.ACCESS.getValue().equals(claims.get("typ"))) {
                log.debug("Token type mismatch: expected access token");
                chain.doFilter(request, response);
                return;
            }

            // 필수 클레임 추출
            Long memberId = ((Number) claims.get("uid")).longValue();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("rol");

            // 5. Spring Security 인증 객체 생성 및 SecurityContext에 저장
            var authentication = new UsernamePasswordAuthenticationToken(
                    memberId,  // principal (사용자 식별자)
                    null,      // credentials (비밀번호 등, JWT에서는 불필요)
                    roles.stream().map(SimpleGrantedAuthority::new).toList()  // authorities (권한)
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for memberId={}", memberId);
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
