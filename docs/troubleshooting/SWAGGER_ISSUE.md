# Swagger에 Auth만 표시되는 문제 해결

## 🔍 문제 상황

Swagger UI에서 AuthController만 보이고 다른 컨트롤러(Product, Cart, Order 등)가 표시되지 않습니다.

## 🎯 원인 분석

### 1. URL 패턴 불일치

**SecurityConfig 설정:**
```java
.authorizeHttpRequests(auth -> auth
    // 인증 불필요 경로
    .requestMatchers(
        "/api/v1/members/login",
        "/api/v1/members/signup",
        "/api/v1/auth/refresh"        // ✅ /api/v1/auth/* 만 permitAll
    ).permitAll()
    
    // 나머지는 모두 인증 필요
    .anyRequest().authenticated()
)
```

**실제 컨트롤러 URL:**
```java
@RequestMapping("/api/v1/auth")     // ✅ AuthController - 일치!
@RequestMapping("/api/products")    // ❌ ProductController - 불일치!
@RequestMapping("/carts")           // ❌ CartController - 불일치!
@RequestMapping("/api/orders")      // ❌ OrderController - 불일치!
```

### 2. Swagger의 동작 방식

```
Swagger UI 접근 플로우:
1. Swagger가 /v3/api-docs 엔드포인트에 접근
2. Spring Boot가 모든 컨트롤러 스캔
3. 각 엔드포인트의 접근 가능 여부 확인
   ├─ /api/v1/auth/* → permitAll ✅
   ├─ /api/products/* → authenticated ❌
   └─ /carts/* → authenticated ❌
4. 접근 가능한 엔드포인트만 Swagger에 표시
```

**결과:**
- Auth 컨트롤러: permitAll이므로 Swagger에 표시됨
- 다른 컨트롤러: authenticated이므로 JWT 토큰 없으면 403 Forbidden

## ✅ 해결 방법

### 방법 1: Swagger에 JWT 인증 설정 추가 (권장)

Swagger UI에서 JWT 토큰을 입력할 수 있도록 설정합니다.

#### 1-1. SwaggerConfig 생성

```java
package com.usinsa.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security 스킴 정의
        String jwtSchemeName = "JWT Authentication";
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);
        
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)")
                );

        return new OpenAPI()
                .info(new Info()
                        .title("Usinsa API")
                        .description("유시나 프로젝트 REST API 문서")
                        .version("v1.0.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
```

#### 1-2. Swagger UI 사용 방법

1. Swagger UI 접속: `http://localhost:8080/swagger-ui/index.html`
2. 우측 상단 **Authorize** 버튼 클릭
3. **JWT Authentication** 필드에 Access Token 입력 (Bearer 제외)
   ```
   예시: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
4. **Authorize** 버튼 클릭
5. 이제 모든 엔드포인트가 표시되고 테스트 가능!

#### 1-3. 토큰 발급 방법

```bash
# 1. 로그인으로 토큰 발급
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}

# 2. 응답에서 accessToken 복사
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  ← 복사
    "refreshToken": "...",
    ...
  }
}

# 3. Swagger UI Authorize에 붙여넣기
```

**장점:**
- ✅ 실제 운영 환경과 동일한 인증 플로우
- ✅ 보안 유지
- ✅ 개발 시 실제 토큰으로 API 테스트 가능

**단점:**
- ⚠️ 매번 로그인해서 토큰 발급 필요
- ⚠️ 토큰 만료 시 재발급 필요

---

### 방법 2: 개발 환경에서 특정 경로 permitAll (비권장)

개발 편의를 위해 임시로 permitAll을 추가합니다.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Swagger 및 개발용 경로
            .requestMatchers(
                "/h2-console/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            
            // ⚠️ 개발 환경에서만 사용!
            .requestMatchers(
                "/api/products/**",    // 상품 API
                "/carts/**",           // 장바구니 API
                "/api/orders/**"       // 주문 API
            ).permitAll()
            
            // 그 외 인증 필요
            .anyRequest().authenticated()
        );
    
    return http.build();
}
```

**장점:**
- ✅ 빠른 개발/테스트 가능
- ✅ 토큰 발급 없이 바로 API 테스트

**단점:**
- ❌ 보안 위험 (운영에 절대 적용 금지!)
- ❌ 실제 인증 플로우 테스트 불가
- ❌ 환경별 설정 관리 필요

---

### 방법 3: Profile별 SecurityConfig 분리 (권장)

개발/운영 환경을 분리하여 관리합니다.

#### 3-1. SecurityConfig 분리

```java
@Configuration
@Profile("local")  // 로컬 개발 환경
public class LocalSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/**").permitAll()  // 모든 경로 허용
        );
        return http.build();
    }
}

@Configuration
@Profile({"dev", "prod"})  // 개발/운영 환경
public class ProductionSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()  // 나머지 인증 필요
        );
        return http.build();
    }
}
```

#### 3-2. Profile 설정

```yaml
# application-local.yml
spring:
  profiles:
    active: local

# application-dev.yml
spring:
  profiles:
    active: dev

# application-prod.yml
spring:
  profiles:
    active: prod
```

**장점:**
- ✅ 환경별 보안 정책 분리
- ✅ 로컬에서 빠른 개발
- ✅ 운영 환경은 안전하게 보호

**단점:**
- ⚠️ 설정 파일 관리 필요
- ⚠️ 로컬에서 인증 플로우 테스트 어려움

---

## 📊 권장 방법 비교

| 방법 | 보안 | 개발 편의성 | 실제 환경 유사도 | 권장도 |
|------|------|------------|----------------|--------|
| 방법 1: Swagger JWT 설정 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ 최우선 권장 |
| 방법 2: permitAll 추가 | ⭐ | ⭐⭐⭐⭐⭐ | ⭐ | ⚠️ 비권장 |
| 방법 3: Profile 분리 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ 권장 |

## 🎯 결론 및 권장사항

### 단계별 적용 전략

#### Phase 1: 즉시 적용 (방법 1)
```
1. SwaggerConfig 생성 (JWT 인증 설정)
2. Swagger UI에서 로그인 → 토큰 발급
3. Authorize 버튼으로 토큰 등록
4. 모든 API 문서 확인 가능
```

**시간**: 10분  
**효과**: 즉시 모든 컨트롤러 표시, 보안 유지

#### Phase 2: 장기 전략 (방법 3)
```
1. Profile별 SecurityConfig 분리
2. 로컬 환경: 인증 완화
3. 개발/운영 환경: 엄격한 인증
4. CI/CD 파이프라인에서 Profile 자동 설정
```

**시간**: 1시간  
**효과**: 환경별 최적화, 유지보수 용이

---

## 🔧 추가 문제 해결

### Q1. Swagger Authorize 버튼이 안 보여요
**답변**: SwaggerConfig의 `addSecurityItem()` 설정 확인

### Q2. 토큰을 입력했는데 401 에러가 나요
**답변**: 
1. 토큰 앞에 "Bearer " 제거했는지 확인
2. 토큰 만료 여부 확인 (30분)
3. 토큰 복사 시 공백 포함 여부 확인

### Q3. 로컬에서 매번 토큰 발급하기 귀찮아요
**답변**: 
- 방법 3 (Profile 분리)를 사용하여 로컬에서는 인증 완화
- 또는 Postman/IntelliJ HTTP Client에 토큰 환경 변수 설정

---

## 📚 참고 자료

- [Swagger with Spring Security](https://www.baeldung.com/spring-security-oauth-swagger)
- [SpringDoc OpenAPI 3.0](https://springdoc.org/#features)
- [Spring Security Profile Configuration](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
