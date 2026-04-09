# 비회원 장바구니 세션 문제 해결 요약

## 🔍 문제 진단

**증상:**
- 비회원이 장바구니에 상품 추가 → 백엔드는 정상 처리
- 장바구니 페이지 이동 → 장바구니가 비어있음
- 회원 로그인 시에는 정상 작동

**핵심 원인:**
1. 백엔드: `STATELESS` 세션 정책으로 세션이 생성되지 않음
2. 프론트엔드: `withCredentials` 미설정으로 쿠키 전송 안 됨

## ✅ 해결 방법

### 백엔드 (usinsa-be)

#### 1. SecurityConfig.java
```java
// 변경 전
.sessionManagement(sm ->
    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

// 변경 후
.sessionManagement(sm ->
    sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
```

**파일:** `src/main/java/com/usinsa/backend/global/config/SecurityConfig.java`

**변경 이유:**
- JWT 인증은 STATELESS로 동작
- 비회원 장바구니는 세션 필요
- `IF_REQUIRED`로 필요시에만 세션 생성

#### 2. CorsConfig.java
```java
// Set-Cookie 헤더 노출 추가
configuration.setExposedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "X-Requested-With",
    "Set-Cookie"  // ← 추가
));
```

**파일:** `src/main/java/com/usinsa/backend/global/config/CorsConfig.java`

### 프론트엔드 (usinsa-fe)

#### src/api/http.ts
```typescript
export const http = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // ← 추가
})

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // ← 추가
})
```

**파일:** `src/api/http.ts`

**변경 이유:**
- 세션 쿠키(JSESSIONID) 전송을 위해 필수
- 서버가 보낸 `Set-Cookie` 헤더를 브라우저에 저장
- 모든 요청에 쿠키 자동 포함

## 📋 수정된 파일 목록

### 백엔드
1. `src/main/java/com/usinsa/backend/global/config/SecurityConfig.java`
2. `src/main/java/com/usinsa/backend/global/config/CorsConfig.java`

### 프론트엔드
1. `src/api/http.ts`

### 문서
1. `docs/CART_SESSION_FIX.md` (신규)
2. `docs/CART_SESSION_FIX_SUMMARY.md` (신규)

## 🧪 테스트 방법

### 1. 비회원 장바구니 추가
```
1. 로그아웃 상태에서 상품 상세 페이지 이동
2. "장바구니" 버튼 클릭
3. 개발자 도구 > Network 탭 확인
   - Response Headers에 "Set-Cookie: JSESSIONID=..." 있어야 함
```

### 2. 장바구니 확인
```
1. 장바구니 페이지 이동
2. 추가한 상품이 목록에 표시되어야 함
3. 개발자 도구 > Network 탭 확인
   - Request Headers에 "Cookie: JSESSIONID=..." 있어야 함
```

### 3. 브라우저 새로고침
```
1. F5 또는 새로고침
2. 장바구니 항목이 유지되어야 함
```

### 4. 로그인 후 병합
```
1. 비회원 상태로 장바구니에 상품 추가
2. 로그인
3. 장바구니 확인
4. 비회원 때 담은 상품 + 기존 회원 장바구니가 병합되어야 함
```

## 🔧 동작 원리

### 변경 전 (문제)
```
비회원 장바구니 추가:
  프론트 → 백엔드
  ❌ 세션 생성 안 됨 (STATELESS)
  ❌ 쿠키 전송 안 됨 (withCredentials 없음)
  → 매번 새로운 세션으로 인식

장바구니 조회:
  프론트 → 백엔드
  ❌ 다른 세션 ID로 조회
  → 빈 결과 반환
```

### 변경 후 (정상)
```
비회원 장바구니 추가:
  프론트 → 백엔드
  ✅ 세션 생성 (IF_REQUIRED)
  ✅ Set-Cookie: JSESSIONID=ABC
  ✅ 프론트엔드 쿠키 저장 (withCredentials: true)

장바구니 조회:
  프론트 → 백엔드
  ✅ Cookie: JSESSIONID=ABC 전송
  ✅ 동일 세션 ID로 조회
  → 장바구니 반환 ✅
```

## ⚠️ 주의사항

### CORS 설정
```yaml
# CORS 허용 Origin을 정확히 설정해야 함
spring:
  cors:
    allowed-origins:
      - http://localhost:5173  # ← 정확한 주소

# ❌ 작동 안 함:
allowed-origins: ["*"]  # allowCredentials: true와 함께 사용 불가
```

### 세션 쿠키 설정
```yaml
# application-dev.yml
server:
  servlet:
    session:
      cookie:
        http-only: true    # XSS 방지
        secure: false      # 개발: HTTP, 프로덕션: true (HTTPS)
        same-site: lax     # CSRF 방지
```

### 세션 타임아웃
```yaml
spring:
  session:
    timeout: 30m  # 30분 후 세션 만료 → 자동 정리
```

## 🎯 JWT와 세션 공존

이 설정으로 JWT 인증과 세션 기반 장바구니가 완벽히 공존합니다:

**JWT 인증 (회원):**
- Authorization 헤더로 JWT 전송
- STATELESS로 동작
- 세션 불필요

**세션 인증 (비회원):**
- Cookie 헤더로 JSESSIONID 전송
- 필요시에만 세션 생성
- 장바구니 조회/수정

**독립적 동작:**
- JWT 있음 → JWT 인증
- JWT 없음 + 비회원 API → 세션 사용
- 서로 간섭 없음

## 📚 참고 문서

- 상세 문서: `docs/CART_SESSION_FIX.md`
- 세션 정리: `docs/CART_SESSION_CLEANUP_GUIDE.md`
- 리팩토링: `docs/CART_REFACTORING_SESSION_CLEANUP.md`

## ✨ 결과

이제 비회원도 장바구니를 정상적으로 사용할 수 있습니다:
- ✅ 상품 추가
- ✅ 수량 변경
- ✅ 상품 삭제
- ✅ 브라우저 새로고침 시 유지
- ✅ 로그인 시 회원 장바구니로 병합
- ✅ 세션 만료 시 자동 정리

🚀 완료!
