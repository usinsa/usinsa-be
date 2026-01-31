# 결제 API 보안 가이드

## 🔒 보안 구조 개요

결제 API는 **다층 방어(Defense in Depth)** 전략을 사용합니다:

1. **SecurityConfig (네트워크 계층)** - URL 기반 접근 제어
2. **JwtAuthenticationFilter (인증 계층)** - JWT 토큰 검증
3. **PaymentService (비즈니스 로직 계층)** - 주문 소유권 검증
4. **Redis TID 저장소 (데이터 계층)** - 결제 상태 관리

## ⚠️ 현재 보안 이슈

### 1. SecurityConfig 테스트 모드 활성화
```java
// SecurityConfig.java - 현재 상태
.requestMatchers("/api/**").permitAll()  // ❌ 모든 API가 인증 없이 접근 가능
```

### 2. 잠재적 공격 시나리오

**시나리오 1: 타인의 주문 결제 시도**
```
공격자가 다른 사용자의 orderId로 결제 준비 시도
→ SecurityConfig 테스트 모드면 통과
→ PaymentService에서 차단 ✅ (주문 소유권 검증)
```

**시나리오 2: 무작위 결제 승인 시도**
```
공격자가 무작위 orderId + pgToken으로 승인 시도
→ SecurityConfig 테스트 모드면 통과
→ PaymentService에서 차단 ✅ (주문 소유권 검증)
```

**시나리오 3: Redis TID 탈취 후 재사용**
```
공격자가 Redis TID를 탈취하여 다른 주문에 사용 시도
→ PaymentService에서 차단 ✅ (TID와 orderId 매핑 확인)
```

## ✅ 구현된 보안 계층

### 1. Service 레벨 보안 (현재 구현됨)

**주문 소유권 검증**
```java
private void validateOrderOwnership(Order order, Long memberId) {
    if (!order.getMember().getId().equals(memberId)) {
        log.warn("주문 소유권 검증 실패: 주문ID={}, 주문소유자={}, 요청자={}", 
                order.getId(), order.getMember().getId(), memberId);
        throw new CustomException(ErrorCode.FORBIDDEN);
    }
}
```

**모든 결제 API에 memberId 파라미터 추가**
- `preparePayment(orderId, memberId)`
- `approvePayment(orderId, pgToken, memberId)`
- `cancelPayment(orderId, memberId)`

### 2. Controller 레벨 보안 (현재 구현됨)

**Authentication 객체에서 memberId 추출**
```java
@PostMapping("/kakao-pay/{orderId}/ready")
public ResponseEntity<KakaoPayDto.ReadyResponse> ready(
        @PathVariable Long orderId,
        Authentication authentication) {  // Spring Security가 자동 주입
    
    Long memberId = getMemberIdFromAuthentication(authentication);
    // ...
}
```

### 3. Redis TID 보안 (현재 구현됨)

**TID-OrderId 매핑**
- TID는 orderId와 1:1 매핑
- 다른 orderId로 TID 재사용 불가
- TTL 30분으로 자동 만료

## 🚀 운영 환경 SecurityConfig 설정

### 권장 설정 (테스트 완료 후 적용)

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
        
        .authorizeHttpRequests(auth -> auth
            // 공개 API
            .requestMatchers(
                "/h2-console/**",
                "/swagger-ui/**",
                "/v3/api-docs/**"
            ).permitAll()
            
            // 인증 관련 API
            .requestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/logout",
                "/api/v1/auth/signup",
                "/api/v1/auth/refresh",
                "/api/v1/auth/oauth/**",
                "/oauth2/authorization/**",
                "/login/oauth2/code/**"
            ).permitAll()
            
            // 상품 조회 API (공개)
            .requestMatchers(HttpMethod.GET,
                "/api/v1/products",
                "/api/v1/products/*",
                "/api/v1/categories",
                "/api/v1/categories/*"
            ).permitAll()
            
            // 검색 API (공개)
            .requestMatchers(HttpMethod.GET,
                "/api/v1/search/**"
            ).permitAll()
            
            // 장바구니 API (비회원 허용 - 세션 기반)
            .requestMatchers("/api/v1/carts/**").permitAll()
            
            // ⭐ 결제 API (인증 필수)
            .requestMatchers("/api/v1/payments/**").authenticated()
            
            // ⭐ 주문 API (인증 필수)
            .requestMatchers("/api/v1/orders/**").authenticated()
            .requestMatchers("/api/v1/ordered-products/**").authenticated()
            
            // ⭐ 회원 정보 API (인증 필수)
            .requestMatchers("/api/v1/members/**").authenticated()
            
            // ⭐ 배송 API (인증 필수)
            .requestMatchers("/api/v1/deliveries/**").authenticated()
            .requestMatchers("/api/v1/delivery-addresses/**").authenticated()
            
            // ⭐ 좋아요 API (인증 필수)
            .requestMatchers("/api/v1/product-likes/**").authenticated()
            
            // 나머지 모든 요청은 인증 필요
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
        );

    http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

## 📊 보안 계층별 책임

| 계층 | 책임 | 실패 시 동작 |
|------|------|------------|
| **SecurityConfig** | URL 기반 접근 제어 | 401 Unauthorized 반환 |
| **JwtAuthenticationFilter** | JWT 토큰 검증, 인증 정보 생성 | 필터 체인 계속 진행 (SecurityConfig에서 차단) |
| **PaymentService** | 주문 소유권 검증, 비즈니스 로직 | 403 Forbidden 예외 발생 |
| **Redis TID Store** | 결제 상태 관리, TID 유효성 | 404 Not Found 예외 발생 |

## 🧪 테스트 시나리오

### 1. 정상 플로우 테스트
```bash
# 1. 로그인하여 JWT 토큰 획득
POST /api/v1/auth/login
Authorization: Bearer {access_token}

# 2. 주문 생성
POST /api/v1/orders
Authorization: Bearer {access_token}

# 3. 결제 준비
POST /api/v1/payments/kakao-pay/1/ready
Authorization: Bearer {access_token}

# 4. 결제 승인
POST /api/v1/payments/kakao-pay/1/approve?pgToken=xxx
Authorization: Bearer {access_token}
```

### 2. 인증 없이 접근 시도 (SecurityConfig 적용 후)
```bash
# JWT 토큰 없이 결제 준비 시도
POST /api/v1/payments/kakao-pay/1/ready

# 예상 응답: 401 Unauthorized
```

### 3. 타인의 주문 결제 시도
```bash
# 사용자 A의 JWT 토큰으로 사용자 B의 주문 결제 시도
POST /api/v1/payments/kakao-pay/999/ready
Authorization: Bearer {user_a_token}

# 예상 응답: 403 Forbidden (주문 소유권 검증 실패)
```

### 4. 만료된 TID로 승인 시도
```bash
# 30분 경과 후 결제 승인 시도
POST /api/v1/payments/kakao-pay/1/approve?pgToken=xxx
Authorization: Bearer {access_token}

# 예상 응답: 404 Not Found (PAYMENT_TID_NOT_FOUND)
```

## 🔐 추가 보안 권장사항

### 1. HTTPS 강제 (운영 환경)
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
  servlet:
    session:
      cookie:
        secure: true  # HTTPS에서만 쿠키 전송
```

### 2. Rate Limiting 적용
```java
// 결제 API에 Rate Limit 적용 (선택사항)
@RateLimiter(name = "payment")
@PostMapping("/kakao-pay/{orderId}/ready")
public ResponseEntity<KakaoPayDto.ReadyResponse> ready(...) {
    // ...
}
```

### 3. 결제 금액 검증
```java
// PaymentService에 금액 검증 로직 추가 (선택사항)
private void validatePaymentAmount(Order order, int requestAmount) {
    int calculatedAmount = calculateTotalAmount(order);
    if (calculatedAmount != requestAmount) {
        throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
}
```

### 4. 결제 이력 로깅
```java
// 모든 결제 시도를 DB에 기록 (선택사항)
@Transactional
public void logPaymentAttempt(Long orderId, Long memberId, String action, String result) {
    PaymentLog log = PaymentLog.builder()
        .orderId(orderId)
        .memberId(memberId)
        .action(action)
        .result(result)
        .timestamp(LocalDateTime.now())
        .build();
    paymentLogRepository.save(log);
}
```

## 🎯 결론

### SecurityConfig만으로 충분한가?

**답: 아니오. 하지만 다층 방어로 보완되었습니다.**

#### SecurityConfig의 역할
- ✅ 첫 번째 방어선: 인증되지 않은 사용자 차단
- ✅ URL 기반 접근 제어
- ⚠️ 주의: 인증된 사용자의 권한 남용은 막을 수 없음

#### 추가 보안 계층의 필요성
1. **PaymentService 소유권 검증** ✅ (구현됨)
   - 인증된 사용자가 타인의 주문을 결제하는 것 방지
   
2. **Redis TID 관리** ✅ (구현됨)
   - TID 재사용 방지
   - 시간 제한 (TTL 30분)
   
3. **Controller Authentication 주입** ✅ (구현됨)
   - Spring Security와 자동 통합
   - 인증 정보 검증

### 현재 구조의 강점

1. **SecurityConfig가 비활성화되어도 안전**
   - Service 계층에서 주문 소유권 검증
   - 타인의 주문 결제 불가
   
2. **JWT 토큰 탈취 시에도 제한적 피해**
   - 해당 사용자의 주문만 결제 가능
   - TID는 orderId와 매핑되어 재사용 불가
   
3. **명확한 에러 메시지**
   - 401: 인증 필요
   - 403: 권한 없음 (주문 소유자 아님)
   - 404: TID 없음

### 운영 체크리스트

- [ ] SecurityConfig 테스트 모드 해제
- [ ] HTTPS 활성화 (운영 환경)
- [ ] 결제 API 인증 필수 설정
- [ ] 결제 로그 모니터링 설정
- [ ] Rate Limiting 검토
- [ ] 주문 금액 검증 로직 추가 검토

## 📚 참고 문서

- [Spring Security Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [카카오페이 보안 가이드](https://developers.kakaopay.com/docs/common/security)
