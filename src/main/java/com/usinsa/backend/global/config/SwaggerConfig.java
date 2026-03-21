package com.usinsa.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 * - JWT 인증 지원
 * - API 문서 자동 생성
 * 
 * Swagger UI 접속: http://localhost:8080/swagger-ui/index.html
 * 
 * 사용 방법:
 * 1. /api/v1/auth/login으로 로그인하여 accessToken 발급
 * 2. Swagger UI 우측 상단 "Authorize" 버튼 클릭
 * 3. JWT Authentication 필드에 accessToken 입력 (Bearer 제외)
 * 4. "Authorize" 버튼 클릭
 * 5. 이제 모든 보호된 API 테스트 가능!
 */
@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME_NAME = "JWT Authentication";

    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 스킴 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .name(JWT_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 제외하고 토큰만 입력하세요.");

        // 보안 요구사항 정의
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(JWT_SCHEME_NAME);

        // Components에 보안 스킴 등록
        Components components = new Components()
                .addSecuritySchemes(JWT_SCHEME_NAME, securityScheme);

        // API 정보 정의
        Info info = new Info()
                .title("Usinsa API")
                .version("v1.0.0")
                .description("""
                        ## 유신사 프로젝트 REST API 문서
                        
                        ### 인증 방법
                        1. POST /api/v1/auth/login - 로그인하여 JWT 토큰 발급
                        2. 우측 상단 "Authorize" 버튼 클릭
                        3. Access Token 입력 (Bearer 제외)
                        4. 인증 완료!
                        
                        ### 토큰 정보
                        - Access Token 유효기간: 30분
                        - Refresh Token 유효기간: 14일
                        
                        ### 문의
                        - GitHub: https://github.com/usinsa/usinsa-be
                        """)
                .contact(new Contact()
                        .name("Usinsa Team")
                        .url("https://github.com/usinsa"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // 서버 정보 정의
        List<Server> servers = List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.usinsa.store")
                        .description("Production Server")
        );

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
