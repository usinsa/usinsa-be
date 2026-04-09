package com.usinsa.backend.global.config;

import com.usinsa.backend.domain.auth.oauth.config.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS(Cross-Origin Resource Sharing) 설정
 * - FE와 BE가 다른 포트/도메인일 때 필수
 * - OAuth 2.0 리다이렉트를 위해 필요
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final OAuth2Properties oAuth2Properties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // OAuth2Properties에서 허용할 Origin 가져오기
        configuration.setAllowedOrigins(Arrays.asList(oAuth2Properties.getAllowedOrigins()));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        // 노출할 헤더 (FE에서 읽을 수 있는 헤더)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Set-Cookie"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
