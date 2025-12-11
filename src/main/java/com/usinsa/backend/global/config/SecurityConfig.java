package com.usinsa.backend.global.config;


import com.usinsa.backend.domain.auth.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.usinsa.backend.domain.auth.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.usinsa.backend.domain.auth.oauth.service.CustomOAuth2UserService;
import com.usinsa.backend.global.filter.JwtAuthenticationFilter;
import com.usinsa.backend.global.security.handler.AuthenticationEntryPointImpl;
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

/**
 * Spring Security 설정
 * - JWT 기반 인증/인가 설정
 * - Stateless 세션 관리
 * - CORS, CSRF 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final CorsConfig corsConfig;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT는 CSRF 공격에 안전)
                .csrf(csrf -> csrf.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

                // 세션을 STATELESS로 설정 (JWT는 서버 세션을 사용하지 않음)
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증 실패 시 커스텀 예외 처리
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint))

                // 요청 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔 접근 허용 (개발 환경)
                        .requestMatchers(
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 인증 불필요 경로
                        .requestMatchers(
                                "/api/v1/members/login",
                                "/api/v1/members/signup",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        // 소셜 로그인 시작 URL 허용
                        .requestMatchers(
                                "/api/v1/auth/oauth/**",
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**"
                        ).permitAll()

                        // GET 요청 공개 경로
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/posts",
                                "/api/v1/posts/*"
                        ).permitAll()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );


        // H2 콘솔 접근을 위한 헤더 설정
        http.headers(h -> h.frameOptions(f -> f.sameOrigin()));

        // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
