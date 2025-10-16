package com.usinsa.backend.global.security;

import com.usinsa.backend.domain.member.entity.Member;
import com.usinsa.backend.domain.member.repository.MemberRepository;
import com.usinsa.backend.domain.member.service.AuthTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;
    private final MemberRepository memberRepository;

    @Value("${custom.jwt.secret-key}")
    private String secretKey;

    private String resolveAccessToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null; // 쿠키/ apiKey 제거
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 로그인, 회원가입 등은 SecurityConfig에서 permitAll 처리. 필터는 전부 통과시키고 유효하면 컨텍스트만 세팅
        String token = resolveAccessToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // payload 파싱 (AuthTokenService에 구현: Ut.Jwt.isValidToken + getPayload 조합)
                Map<String, Object> payload = authTokenService.getPayload(token);
                if (payload != null) {
                    String username = (String) payload.get("usinaId"); // 또는 "email"/"username" 등 네가 넣은 클레임 키
                    if (username == null) username = String.valueOf(payload.get("id"));

                    // DB 조회 (정확한 principal로 통일: email 추천)
                    Member m = memberRepository.findByUsinaId(username)
                            .orElse(null); // email 기반이면 findByEmail로 변경

                    if (m != null) {
                        UserDetails user = User.withUsername(m.getEmail()) // principal은 email로 통일 권장
                                .password(m.getPassword())
                                .authorities(m.getIsAdmin() ? "ROLE_ADMIN" : "ROLE_USER")
                                .build();

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // 유효하지 않은 토큰 -> 그냥 인증 없이 통과 (요청이 보호된 리소스면 EntryPoint가 401)
            }
        }
        chain.doFilter(request, response);
    }
}