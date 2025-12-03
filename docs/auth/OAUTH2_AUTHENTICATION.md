# OAuth2 인증 아키텍처 및 상세 명세서

## 📋 목차
1. [개요](#개요)
2. [OAuth2 인증 플로우](#oauth2-인증-플로우)
3. [핵심 컴포넌트](#핵심-컴포넌트)
4. [OAuth2 제공자별 정보 처리](#oauth2-제공자별-정보-처리)
5. [테스트 전략](#테스트-전략)

---

## 개요

### 목적
Usinsa 프로젝트에 소셜 로그인(OAuth2) 기능을 통합하여 사용자 편의성을 높입니다. Google, Naver, Kakao 등 다양한 OAuth2 제공자를 지원하며, 기존의 JWT 기반 인증 시스템과 원활하게 연동하는 것을 목표로 합니다.

### 주요 기능
- Google, Naver, Kakao를 이용한 소셜 로그인
- 신규 사용자의 경우, 소셜 계정 정보 기반 자동 회원가입
- 기존 사용자의 경우, 소셜 계정과 기존 계정 연동 (이메일 기반)
- 인증 성공 시, 기존 시스템과 동일한 JWT(Access Token, Refresh Token) 발급

---

## OAuth2 인증 플로우

```
클라이언트                Spring Security         OAuth2 Provider         Usinsa 서버
    │                          │                        │                      │
    ├─ GET /oauth2/authorization/{provider} ────────> │                        │
    │  (e.g., /oauth2/authorization/google)           │                        │
    │                                                 │                        │
    │                          ├─ Provider에 인증 요청 ──> │
    │                          │                        │                      │
    │                          │ <─ 인증 코드 전달 ───────┤
    │                          │                        │                      │
    │ <─ Provider 로그인 페이지로 리다이렉트 ───────────────┤
    │                                                 │                        │
    │  (사용자 로그인 및 권한 동의)                   │                        │
    │                                                 │                        │
    ├─ Provider가 Callback URL로 리다이렉트 ────────────> │
    │  /login/oauth2/code/{provider}?code=...         │                        │
    │                                                 │                        │
    │                          ├─ Access Token 요청 ────> │
    │                          │                        │                      │
    │                          │ <─ Access Token 응답 ────┤
    │                          │                        │                      │
    │                          ├─ 사용자 정보 요청 ──────> │
    │                          │                        │                      │
    │                          │ <─ 사용자 정보 응답 ────┤
    │                          │                        │                      │
    │                          ├─ CustomOAuth2UserService 호출 ──────────────> │
    │                          │                        │                      │
    │                          │                        │  - 사용자 정보 기반 회원 조회/가입
    │                          │                        │  - PrincipalDetails 생성
    │                          │ <─ PrincipalDetails ─── │
    │                                                 │
    ├─ OAuth2AuthenticationSuccessHandler 호출 ─────────> │
    │                                                 │  - PrincipalDetails에서 회원 정보 획득
    │                                                 │  - JWT (Access/Refresh) 토큰 발급
    │                                                 │
    ├─ 지정된 URL로 리다이렉트 (JWT 토큰 포함) ──────────> │
    │  http://localhost:3000/oauth/redirect?accessToken=...&refreshToken=...
    │
    │ (클라이언트는 URL에서 토큰을 파싱하여 저장)
```

---

## 핵심 컴포넌트

### 1. SecurityConfig
- `oauth2Login()` 설정을 통해 OAuth2 로그인 프로세스를 활성화합니다.
- `userInfoEndpoint()`: 사용자 정보를 가져온 후 처리할 `customOAuth2UserService`를 지정합니다.
- `successHandler()`: 인증 성공 후 로직을 처리할 `oAuth2AuthenticationSuccessHandler`를 지정합니다.
- `failureHandler()`: 인증 실패 시 로직을 처리할 `oAuth2AuthenticationFailureHandler`를 지정합니다.

### 2. CustomOAuth2UserService
- `DefaultOAuth2UserService`를 상속받아 `loadUser()` 메서드를 오버라이드합니다.
- OAuth2 제공자로부터 받은 사용자 정보를 기반으로 다음을 수행합니다.
    1.  `OAuth2UserInfoFactory`를 통해 제공자별(`google`, `naver`, `kakao`)로 사용자 정보를 파싱합니다.
    2.  이메일과 제공자 정보로 `MemberRepository`에서 기존 회원을 조회합니다.
    3.  **기존 회원이 없는 경우 (신규 사용자):**
        -   제공자 정보(`provider`, `providerId`)와 이메일, 이름, 프로필 이미지 등을 기반으로 새로운 `Member` 객체를 생성합니다.
        -   `usinaId`는 `{provider}_{providerId}` 형식으로 생성됩니다. (예: `google_1029...`)
        -   비밀번호는 사용되지 않으므로, UUID를 이용한 임의의 값으로 채워집니다.
        -   `memberRepository.save()`를 통해 DB에 저장합니다.
    4.  **기존 회원이 있는 경우:**
        -   해당 회원 정보를 그대로 사용합니다.
    5.  조회 또는 생성된 `Member` 객체를 기반으로 `PrincipalDetails` 객체를 생성하여 반환합니다.

### 3. OAuth2AuthenticationSuccessHandler
- `SimpleUrlAuthenticationSuccessHandler`를 상속받아 `onAuthenticationSuccess()` 메서드를 오버라이드합니다.
- 인증 완료 후 `Authentication` 객체에서 `PrincipalDetails`를 가져옵니다.
- `PrincipalDetails`에 포함된 `Member` 정보를 `JwtTokenService`에 전달하여 **Access Token**과 **Refresh Token**을 발급받습니다.
- 프론트엔드에서 지정한 리다이렉트 URL(예: `http://localhost:3000/oauth/redirect`)에 토큰들을 쿼리 파라미터로 추가하여 리다이렉트시킵니다.

### 4. OAuth2UserInfo 및 하위 클래스
- `OAuth2UserInfo` (추상 클래스): 제공자별로 상이한 사용자 정보 응답을 표준화하기 위한 추상 계층입니다. `getId()`, `getName()`, `getEmail()` 등의 추상 메서드를 정의합니다.
- `GoogleUserInfo`, `NaverUserInfo`, `KakaoUserInfo`: 각 제공자의 응답 `attributes` 구조에 맞게 `OAuth2UserInfo`를 구현한 클래스입니다.
- `OAuth2UserInfoFactory`: `registrationId`(`google`, `naver`, `kakao`)에 따라 적절한 `OAuth2UserInfo` 구현체를 반환하는 팩토리 클래스입니다.

---

## OAuth2 제공자별 정보 처리

### `application.yml` 설정
- `spring.security.oauth2.client.registration.{provider}` 하위에 각 제공자별 `client-id`, `client-secret`, `scope` 등을 설정합니다.
- **주의:** `client-id`와 `client-secret`은 민감 정보이므로, 실제 운영 환경에서는 환경 변수나 외부 설정 파일을 통해 주입해야 합니다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: "YOUR_GOOGLE_CLIENT_ID"
            client-secret: "YOUR_GOOGLE_CLIENT_SECRET"
            scope:
              - profile
              - email
          naver:
            client-id: "YOUR_NAVER_CLIENT_ID"
            client-secret: "YOUR_NAVER_CLIENT_SECRET"
            # ... (기타 설정)
          kakao:
            client-id: "YOUR_KAKAO_CLIENT_ID"
            client-secret: "YOUR_KAKAO_CLIENT_SECRET"
            # ... (기타 설정)
```

### 제공자별 `scope` 및 사용자 정보 매핑

- **Google**:
    - `scope`: `profile`, `email`
    - `id`: `sub`
    - `email`: `email`
    - `name`: `name`
- **Naver**:
    - `scope`: `name`, `email`, `profile_image`
    - 응답이 `response` 객체 내부에 한 단계 더 감싸여 있습니다.
    - `id`: `response.id`
    - `email`: `response.email`
    - `name`: `response.name`
- **Kakao**:
    - `scope`: `profile_nickname`, `profile_image`, `account_email`
    - `id`: `id`
    - `email`: `kakao_account.email`
    - `name`: `properties.nickname`

---

## 테스트 전략

### 단위 테스트
- `CustomOAuth2UserServiceTest`:
    - `@ExtendWith(MockitoExtension.class)`를 사용하여 단위 테스트 환경을 구성합니다.
    - `MemberRepository`를 Mocking하여 DB 의존성을 제거합니다.
    - `spy`를 사용하여 `super.loadUser()`의 실제 호출을 막고, 미리 정의된 `OAuth2User` 객체를 반환하도록 설정합니다.
    - **시나리오 1: 신규 사용자**
        - `memberRepository.findByEmailAndOauthProvider()`가 `Optional.empty()`를 반환하도록 설정합니다.
        - `memberRepository.save()`가 호출되는지, 그리고 반환된 `PrincipalDetails`에 올바른 정보가 담겨 있는지 검증합니다.
    - **시나리오 2: 기존 사용자**
        - `memberRepository.findByEmailAndOauthProvider()`가 특정 `Member`를 포함한 `Optional`을 반환하도록 설정합니다.
        - `memberRepository.save()`가 호출되지 않는지, 그리고 반환된 `PrincipalDetails`가 기존 회원 정보를 담고 있는지 검증합니다.

### 통합 테스트
- 실제 OAuth2 제공자와의 연동 테스트는 로컬 환경에서 수동으로 진행합니다.
- `application-dev.yml`에 실제 `client-id`와 `client-secret`을 설정하고, 각 소셜 로그인을 직접 실행하여 JWT 토큰이 정상적으로 발급되고 리다이렉트되는지 확인합니다.
- 발급받은 JWT 토큰으로 기존의 인증이 필요한 API를 호출하여 정상적으로 응답하는지 검증합니다.
