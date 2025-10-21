package com.usinsa.backend.global.config;

import com.usinsa.backend.global.security.AuthenticationEntryPointImpl;
import com.usinsa.backend.global.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CSRF 비활성화 (JWT 환경에서는 세션을 사용하지 않기 때문에)
                .csrf(csrf -> csrf.disable())
                
                // ✅ CORS 설정 (기본 허용, 필요 시 별도 WebMvcConfigure로 분리)
                .cors(cors -> {})
                
                // ✅ 세션을 STATELESS로 설정 - JWT는 서버 세션을 사용하지 않기 때문에
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // jwt는 세션 형태가 아니기 때문에 STATELESS 구현
                
                // ✅ 인증 실패 시 커스텀 예외 처리
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                
                // ✅ 요청 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 공개 경로(인증 없이 접근 가능)
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/v1/members/login", "/api/v1/members/signup", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/*").permitAll()
                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                );

        // ✅ H2 콘솔 접근 허용 (개발 환경용)
        http.headers(h -> h.frameOptions(f -> f.sameOrigin())); // H2 콘솔용

        // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 추가)
        // 요청 시 JWT 토큰을 먼저 검증하고 SecurityContext에 인증 정보 저장
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
