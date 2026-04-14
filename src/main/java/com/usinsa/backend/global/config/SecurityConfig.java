package com.usinsa.backend.global.config;

import com.usinsa.backend.domain.auth.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.usinsa.backend.domain.auth.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.usinsa.backend.domain.auth.oauth.repository.CookieOAuth2AuthorizationRequestRepository;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))

            // 세션 정책: NEVER — 기존 세션은 사용하되 새로 생성하지 않음
            // 비회원 장바구니는 요청 측에서 X-Session-Id 헤더로 직접 전달하는 방식으로 분리
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.NEVER))

            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 인증 API
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/logout",
                    "/api/v1/auth/signup",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/me",
                    "/api/v1/products/reindex"
                ).permitAll()
                // OAuth2
                .requestMatchers(
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**"
                ).permitAll()
                // 상품/카테고리 조회 — 비로그인 허용
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/products/**",
                    "/api/v1/categories/**",
                    "/api/v1/search/**"
                ).permitAll()
                // 비회원 장바구니
                .requestMatchers("/api/v1/carts/guest/**").permitAll()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ep -> ep
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );

        http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
