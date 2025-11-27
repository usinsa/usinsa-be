# Auth 도메인 아키텍처 및 상세 명세서

## 📋 목차
1. [개요](#개요)
2. [아키텍처 설계 원칙](#아키텍처-설계-원칙)
3. [JWT 토큰 구조](#jwt-토큰-구조)
4. [핵심 컴포넌트](#핵심-컴포넌트)
5. [인증 플로우](#인증-플로우)
6. [보안 메커니즘](#보안-메커니즘)
7. [Redis 활용 전략](#redis-활용-전략)

---

## 개요

### 목적
Usinsa 프로젝트의 인증/인가 시스템을 담당하는 도메인입니다. JWT 기반의 Stateless 인증 방식을 채택하되, 보안 강화를 위해 Refresh Token과 블랙리스트는 Redis에 저장합니다.

### 주요 기능
- **로그인**: 이메일/비밀번호 기반 인증 후 JWT 발급
- **토큰 갱신**: Refresh Token을 이용한 Access Token 재발급 (Rotation 방식)
- **로그아웃**: Access Token 블랙리스트 등록
- **멀티 디바이스 지원**: 디바이스별 독립적인 세션 관리

---

## 아키텍처 설계 원칙

### 1. Stateless 원칙
```
┌─────────────────────────────────────────────────────────────┐
│  Stateless 설계 철학                                          │
├─────────────────────────────────────────────────────────────┤
│  Access Token  → 완전한 Stateless (서버 상태 불필요)         │
│  Refresh Token → 메타정보만 Redis 저장 (보안 강화)           │
│  Blacklist     → 로그아웃 처리용 최소한의 상태 관리          │
└─────────────────────────────────────────────────────────────┘
```

**왜 Stateless인가?**
- **수평 확장 용이**: 서버 간 세션 공유 불필요
- **성능**: 매 요청마다 DB 조회 없이 토큰만으로 인증
- **MSA 친화적**: 서비스 간 인증 정보 전달 용이

**왜 Refresh Token은 Redis에 저장하는가?**
- Access Token은 짧은 수명(30분)으로 탈취 위험 최소화
- Refresh Token은 긴 수명(14일)이므로 서버 측 검증 필수
- Redis 저장으로 즉시 무효화 가능 (로그아웃, 보안 이슈 발생 시)

### 2. 보안 우선 설계
```
계층별 보안 전략:
├─ Transport Layer: HTTPS 필수
├─ Token Layer: HS256 서명, JTI 기반 추적
├─ Storage Layer: Redis TTL 자동 만료
└─ Application Layer: Rotation, Blacklist
```

---

## JWT 토큰 구조

### Access Token
```json
{
  "uid": 1,                    // 사용자 ID (User ID)
  "rol": ["ROLE_USER"],        // 권한 목록 (Roles)
  "jti": "uuid-v4",            // JWT ID (고유 식별자)
  "typ": "access",             // 토큰 타입
  "iat": 1234567890,           // 발급 시간 (Issued At)
  "exp": 1234569690            // 만료 시간 (30분 후)
}
```

**필드별 상세 설명:**

| 필드 | 타입 | 설명 | 왜 필요한가? |
|------|------|------|-------------|
| `uid` | Long | 회원 고유 ID | 인증된 사용자 식별, SecurityContext에 저장 |
| `rol` | List<String> | 권한 목록 | Spring Security의 권한 기반 접근 제어 (RBAC) |
| `jti` | String | JWT 고유 ID | 블랙리스트 관리 시 토큰 추적용 (로그아웃) |
| `typ` | String | 토큰 타입 | Access/Refresh 구분하여 잘못된 토큰 사용 방지 |
| `iat` | Long | 발급 시간 | 토큰 생성 시점 추적, 디버깅 |
| `exp` | Long | 만료 시간 | 토큰 자동 만료 처리 |

### Refresh Token
```json
{
  "uid": 1,                    // 사용자 ID
  "jti": "uuid-v4",            // JWT ID
  "dev": "device-abc123",      // 디바이스 ID
  "typ": "refresh",            // 토큰 타입
  "iat": 1234567890,           // 발급 시간
  "exp": 1235777690            // 만료 시간 (14일 후)
}
```

**필드별 상세 설명:**

| 필드 | 타입 | 설명 | 왜 필요한가? |
|------|------|------|-------------|
| `uid` | Long | 회원 고유 ID | 토큰 갱신 시 사용자 식별 |
| `jti` | String | JWT 고유 ID | Rotation 시 재사용 공격 탐지 (중요!) |
| `dev` | String | 디바이스 ID | 멀티 디바이스 세션 관리 |
| `typ` | String | 토큰 타입 | Access Token으로 잘못 사용 방지 |

---

## 핵심 컴포넌트

### 1. JwtTokenService

#### 역할
JWT 토큰의 생명주기 전체를 관리하는 고수준 서비스입니다.

#### 주요 메서드

##### issueTokens()
```java
public TokenPair issueTokens(Long memberId, String email, 
                             List<String> roles, String deviceId)
```

**동작 과정:**
```
1. 현재 시간 기준 만료 시간 계산
2. Access Token 생성
   ├─ uid, rol, jti, typ 클레임 추가
   └─ 30분 만료 시간 설정
3. Refresh Token 생성
   ├─ uid, jti, dev, typ 클레임 추가
   └─ 14일 만료 시간 설정
4. Refresh Token 메타정보 Redis 저장
   ├─ Key: auth:refresh:{memberId}:{deviceId}
   ├─ Value: {jti, email, roles, expiresAt}
   └─ TTL: 14일
5. TokenPair 반환
```

**왜 Refresh Token 메타정보를 저장하는가?**
- **Rotation 검증**: 토큰 재사용 공격 탐지 (같은 JTI가 두 번 사용되면 공격으로 간주)
- **즉시 무효화**: 보안 이슈 발생 시 Redis에서 삭제하여 즉시 차단
- **회원 정보 캐싱**: 토큰 갱신 시 DB 조회 없이 Redis에서 빠르게 조회

##### rotateTokens()
```java
public TokenPair rotateTokens(String refreshToken, String deviceId)
```

**동작 과정 (Refresh Token Rotation):**
```
1. Refresh Token 파싱 및 검증
   ├─ 서명 검증
   ├─ 만료 시간 확인
   └─ 토큰 타입 확인 (refresh 여부)

2. Redis에서 최신 메타정보 조회
   └─ Key: auth:refresh:{uid}:{deviceId}

3. JTI 비교 (핵심 보안 로직!)
   ├─ Redis JTI == Token JTI → 정상
   └─ Redis JTI != Token JTI → 재사용 공격!
       └─ Redis 데이터 즉시 삭제
       └─ TOKEN_REUSED 예외 발생

4. 새로운 토큰 쌍 발급
   └─ 기존 Refresh Token 메타정보 덮어쓰기
```

**Refresh Token Rotation이란?**
- 토큰 갱신 시 새로운 Refresh Token도 함께 발급
- 기존 Refresh Token은 즉시 무효화
- 재사용 공격(Replay Attack) 방지

**재사용 공격 시나리오:**
```
정상 사용자: Refresh Token으로 갱신 요청
서버: 새 토큰 발급 + Redis JTI 업데이트

공격자: 탈취한 구 Refresh Token으로 갱신 시도
서버: Redis JTI != Token JTI 감지
     → 보안 위협으로 판단
     → 해당 디바이스 모든 세션 무효화
```

##### logout()
```java
public void logout(String accessToken)
```

**동작 과정:**
```
1. Access Token 파싱
2. JTI 추출
3. Redis 블랙리스트 등록
   ├─ Key: auth:blacklist:{jti}
   ├─ Value: "1"
   └─ TTL: 토큰 만료 시간까지
```

**왜 블랙리스트가 필요한가?**
- JWT는 Stateless이므로 서버가 임의로 무효화 불가
- 만료 전까지 유효한 토큰이 탈취되면 위험
- 로그아웃 시 즉시 해당 토큰 사용 차단 필요

**TTL을 토큰 만료 시간까지만 설정하는 이유:**
- 만료된 토큰은 어차피 사용 불가
- Redis 메모리 효율적 관리

##### resolveDeviceId()
```java
public String resolveDeviceId(HttpServletRequest request)
```

**Device ID 추출 전략:**
```
1. X-Device-Id 헤더 확인
   └─ 클라이언트가 UUID 등을 직접 전송

2. 헤더 없으면 User-Agent 해시값 사용
   └─ Objects.hash(userAgent)
   └─ 동일 브라우저/앱은 동일 Device ID
```

**왜 Device ID가 필요한가?**

**멀티 디바이스 시나리오:**
```
사용자 홍길동:
├─ 스마트폰 (deviceId: phone-123)
│  └─ Refresh Token: RT-A
├─ 노트북 (deviceId: laptop-456)
│  └─ Refresh Token: RT-B
└─ 태블릿 (deviceId: tablet-789)
   └─ Refresh Token: RT-C

각 디바이스는 독립적인 세션 유지
한 디바이스 로그아웃해도 다른 디바이스 영향 없음
```

**Redis 저장 구조:**
```
auth:refresh:1:phone-123   → {jti: RT-A, ...}
auth:refresh:1:laptop-456  → {jti: RT-B, ...}
auth:refresh:1:tablet-789  → {jti: RT-C, ...}
```

---

## 인증 플로우

### 1. 로그인 플로우
```
클라이언트                서버                  DB         Redis
    │                      │                    │           │
    ├─ POST /login ────────>│                    │           │
    │  {email, password}    │                    │           │
    │                       │                    │           │
    │                       ├─ 회원 조회 ───────>│           │
    │                       │<─ Member ──────────┤           │
    │                       │                    │           │
    │                       ├─ 비밀번호 검증     │           │
    │                       │  (BCrypt)          │           │
    │                       │                    │           │
    │                       ├─ Device ID 추출    │           │
    │                       │  (Header/UA)       │           │
    │                       │                    │           │
    │                       ├─ JWT 토큰 생성     │           │
    │                       │  - Access Token    │           │
    │                       │  - Refresh Token   │           │
    │                       │                    │           │
    │                       ├─ Refresh 메타 저장 ────────────>│
    │                       │  Key: auth:refresh:{uid}:{dev} │
    │                       │  TTL: 14일         │           │
    │                       │                    │           │
    │<─ 200 OK ─────────────┤                    │           │
    │  {accessToken,        │                    │           │
    │   refreshToken,       │                    │           │
    │   memberInfo}         │                    │           │
```

### 2. 인증된 요청 플로우
```
클라이언트                Filter              SecurityContext    Controller
    │                      │                        │              │
    ├─ GET /api/... ───────>│                        │              │
    │  Authorization:       │                        │              │
    │  Bearer {accessToken} │                        │              │
    │                       │                        │              │
    │                       ├─ 토큰 추출             │              │
    │                       ├─ 서명 검증             │              │
    │                       ├─ 만료 시간 확인        │              │
    │                       ├─ 블랙리스트 확인       │              │
    │                       │  (Redis)              │              │
    │                       │                        │              │
    │                       ├─ Claims 파싱          │              │
    │                       │  {uid, rol, ...}      │              │
    │                       │                        │              │
    │                       ├─ Authentication 생성 ─>│              │
    │                       │  Principal: memberId   │              │
    │                       │  Authorities: roles    │              │
    │                       │                        │              │
    │                       │                        ├─ 요청 처리 ─>│
    │                       │                        │              │
    │                       │                        │<─ 응답 ──────┤
    │<─ 200 OK ─────────────┤<───────────────────────┤              │
```

### 3. 토큰 갱신 플로우 (Rotation)
```
클라이언트                서버                Redis
    │                      │                  │
    ├─ POST /refresh ──────>│                  │
    │  {refreshToken}       │                  │
    │                       │                  │
    │                       ├─ RT 파싱         │
    │                       │  {uid, jti, dev} │
    │                       │                  │
    │                       ├─ 메타정보 조회 ──>│
    │                       │<─ {stored_jti} ──┤
    │                       │                  │
    │                       ├─ JTI 비교        │
    │                       │  stored == token?│
    │                       │  ✅ 정상          │
    │                       │                  │
    │                       ├─ 새 토큰 쌍 생성 │
    │                       │  - New AT        │
    │                       │  - New RT        │
    │                       │                  │
    │                       ├─ 메타 업데이트 ──>│
    │                       │  new_jti 저장    │
    │                       │                  │
    │<─ 200 OK ─────────────┤                  │
    │  {newAccessToken,     │                  │
    │   newRefreshToken}    │                  │
```

**재사용 공격 탐지:**
```
공격자가 탈취한 구 RT 사용 시:
    │                       │                  │
    ├─ POST /refresh ──────>│                  │
    │  {old_refreshToken}   │                  │
    │                       │                  │
    │                       ├─ RT 파싱         │
    │                       │  jti: old_jti    │
    │                       │                  │
    │                       ├─ 메타정보 조회 ──>│
    │                       │<─ {new_jti} ─────┤
    │                       │                  │
    │                       ├─ JTI 비교        │
    │                       │  old != new      │
    │                       │  ❌ 재사용 공격!  │
    │                       │                  │
    │                       ├─ 세션 무효화 ────>│
    │                       │  DELETE key      │
    │                       │                  │
    │<─ 401 TOKEN_REUSED ───┤                  │
```

### 4. 로그아웃 플로우
```
클라이언트                서버                Redis
    │                      │                  │
    ├─ POST /logout ───────>│                  │
    │  Authorization:       │                  │
    │  Bearer {accessToken} │                  │
    │                       │                  │
    │                       ├─ AT 추출         │
    │                       ├─ 파싱 {jti, exp} │
    │                       │                  │
    │                       ├─ 블랙리스트 등록 ─>│
    │                       │  Key: auth:blacklist:{jti}
    │                       │  Value: "1"      │
    │                       │  TTL: exp - now  │
    │                       │                  │
    │<─ 200 OK ─────────────┤                  │
```

---

## 보안 메커니즘

### 1. Refresh Token Rotation

**개념:**
- 토큰 갱신 시마다 새로운 Refresh Token 발급
- 구 Refresh Token 즉시 무효화
- 재사용 시도 시 모든 세션 무효화

**구현:**
```java
// JwtTokenService.rotateTokens()
var latest = refreshStore.find(uid, deviceId);
if (!Objects.equals(latest.getJti(), jti)) {
    // 재사용 공격 탐지!
    refreshStore.delete(uid, deviceId);
    throw new CustomException(ErrorCode.TOKEN_REUSED);
}
```

**보안 이점:**
- 토큰 탈취 시 피해 최소화
- 공격자가 한 번만 사용 가능
- 정상 사용자의 갱신으로 공격자 토큰 무효화

### 2. 블랙리스트 (Logout)

**개념:**
- 로그아웃된 Access Token의 JTI를 Redis에 저장
- 해당 토큰 사용 시 401 에러 반환
- TTL을 토큰 만료 시간까지만 설정

**구현:**
```java
// JwtAuthenticationFilter.doFilterInternal()
if (tokenService.isBlacklisted(token)) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}
```

**왜 Access Token만 블랙리스트?**
- Refresh Token은 갱신 시 Redis에서 직접 검증
- Access Token은 Stateless이므로 블랙리스트 필요

### 3. 멀티 디바이스 세션 관리

**Device ID 기반 독립 세션:**
```
User ID 1:
├─ Device A: RT-A (독립 세션)
├─ Device B: RT-B (독립 세션)
└─ Device C: RT-C (독립 세션)

Device A 로그아웃 → RT-A만 무효화
Device B, C는 영향 없음
```

---

## Redis 활용 전략

### 1. Refresh Token Store

**Key 구조:**
```
auth:refresh:{memberId}:{deviceId}
```

**Value (Hash):**
```json
{
  "jti": "uuid",
  "memberId": "1",
  "email": "user@example.com",
  "roles": "ROLE_USER,ROLE_ADMIN",
  "deviceId": "device-123",
  "expEpoch": "1735689600"
}
```

**TTL:**
- Refresh Token 만료 시간 (14일)
- 자동 삭제로 Redis 메모리 효율 관리

**왜 Hash 타입?**
- 구조화된 데이터 저장
- 부분 업데이트 가능 (필요 시)
- 메모리 효율적

### 2. Blacklist

**Key 구조:**
```
auth:blacklist:{jti}
```

**Value:**
```
"1"
```

**TTL:**
- Access Token 만료 시간 (30분)
- 만료 후 자동 삭제

**왜 String 타입?**
- 단순 존재 여부만 확인
- 메모리 최소화

### 3. Redis 장애 대응

**현재 구현:**
- Redis 장애 시 Refresh Token 갱신 실패
- 로그인은 가능 (Redis는 쓰기만 실패)

**개선 방안 (향후):**
```java
@Cacheable(cacheNames = "refreshToken", 
           unless = "#result == null")
public Optional<TokenMeta> find(Long memberId, String deviceId) {
    // Cache Miss 시 DB 조회 (fallback)
}
```

---

## 성능 최적화

### 1. JWT 파싱 캐싱
```java
// 현재: 매 요청마다 파싱
Claims claims = JwtUtil.parse(secret, token);

// 개선안: Claims 캐싱 (향후)
@Cacheable(key = "#token")
public Claims parseCached(String token) {
    return JwtUtil.parse(secret, token);
}
```

### 2. Redis Pipeline
```java
// 개선안: 대량 조회 시 파이프라인 사용
List<TokenMeta> metas = redis.executePipelined(...);
```

---

## 예외 처리

### ErrorCode 정의
```java
// 인증 관련
UNAUTHORIZED(401, "AUTH_001", "인증이 필요합니다")
TOKEN_INVALID(401, "AUTH_002", "유효하지 않은 토큰입니다")
TOKEN_EXPIRED(401, "AUTH_003", "만료된 토큰입니다")
TOKEN_TYPE_MISMATCH(401, "AUTH_004", "토큰 타입이 일치하지 않습니다")
TOKEN_REVOKED(401, "AUTH_005", "폐기된 토큰입니다")
TOKEN_REUSED(401, "AUTH_006", "재사용된 토큰입니다 (보안 위협)")

// 회원 관련
MEMBER_NOT_FOUND(404, "MEMBER_001", "회원을 찾을 수 없습니다")
```

---

## 테스트 전략

### 단위 테스트
- JwtUtil: 토큰 생성/파싱 테스트
- JwtTokenService: 각 메서드 단위 테스트

### 통합 테스트
- 로그인 → 인증 요청 → 로그아웃 전체 플로우
- 토큰 갱신 Rotation 테스트
- 재사용 공격 시나리오 테스트

### 보안 테스트
- 만료 토큰 사용 시도
- 잘못된 서명 토큰 사용 시도
- 블랙리스트 토큰 사용 시도

---

## 참고 자료
- [RFC 7519 - JWT](https://tools.ietf.org/html/rfc7519)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture)
