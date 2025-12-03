# Usinsa Backend 기술 문서

## 📚 문서 목록

### 인증 (Auth) 도메인
- **[AUTH_ARCHITECTURE.md](./auth/AUTH_ARCHITECTURE.md)**: Auth 도메인 전체 아키텍처 및 상세 명세
  - JWT 토큰 구조 상세 설명
  - Refresh Token Rotation 메커니즘
  - 블랙리스트 관리 전략
  - Redis 활용 방법
  - 보안 메커니즘
  - Device ID의 필요성과 활용

### 회원 (Member) 도메인
- **[MEMBER_DOMAIN.md](./auth/MEMBER_DOMAIN.md)**: Member 엔티티 상세 명세
  - 필드별 상세 설명
  - 연관 관계 설명
  - 비즈니스 로직

## 🎯 문서 작성 목적

이 문서들은 다음을 목적으로 작성되었습니다:

1. **신규 개발자 온보딩**: 프로젝트 구조와 설계 의도를 빠르게 이해
2. **코드 이해도 향상**: 각 컴포넌트가 왜 필요한지, 어떻게 동작하는지 명확히 설명
3. **의사 결정 근거 문서화**: 설계 선택의 이유와 트레이드오프 기록
4. **유지보수성 향상**: 시간이 지나도 코드의 의도를 파악 가능

## 📖 문서 읽는 순서

### Auth 도메인을 처음 접하는 경우
1. **AUTH_ARCHITECTURE.md** 시작
   - "개요" 섹션: 전체 그림 파악
   - "JWT 토큰 구조" 섹션: 필드별 상세 설명
   - "핵심 컴포넌트" 섹션: JwtTokenService 메서드 이해
   - "인증 플로우" 섹션: 시퀀스 다이어그램으로 흐름 파악

2. **코드 읽기**
   - `JwtTokenService.java`: 핵심 비즈니스 로직
   - `JwtAuthenticationFilter.java`: 요청 처리 흐름
   - 테스트 코드: 실제 사용 예시

### Member 도메인을 처음 접하는 경우
1. **MEMBER_DOMAIN.md**
   - "필드별 상세 설명": 각 필드의 목적과 사용법
   - "연관 관계": Order, Cart 등과의 관계

## 🔍 자주 묻는 질문 (FAQ)

### Q1. Device ID가 왜 필요한가요?
**답변**: [AUTH_ARCHITECTURE.md - Device ID 섹션](./auth/AUTH_ARCHITECTURE.md#왜-device-id가-필요한가) 참고

멀티 디바이스 세션 관리를 위해 필요합니다:
- 사용자가 스마트폰, 노트북, 태블릿 등 여러 기기에서 동시 로그인
- 각 기기별로 독립적인 세션 관리
- 한 기기에서 로그아웃해도 다른 기기는 영향 없음

### Q2. 왜 블랙리스트를 Redis에 저장하나요?
**답변**: [AUTH_ARCHITECTURE.md - 블랙리스트 섹션](./auth/AUTH_ARCHITECTURE.md#2-블랙리스트-logout) 참고

- JWT는 Stateless이므로 서버가 임의로 무효화 불가
- 로그아웃 시 즉시 토큰 차단 필요
- Redis TTL로 자동 만료 처리 (메모리 효율적)

### Q3. Refresh Token Rotation이 뭔가요?
**답변**: [AUTH_ARCHITECTURE.md - Rotation 섹션](./auth/AUTH_ARCHITECTURE.md#1-refresh-token-rotation) 참고

- 토큰 갱신 시 새로운 Refresh Token도 함께 발급
- 기존 토큰 즉시 무효화
- 재사용 공격(Replay Attack) 방지

### Q4. Member의 id를 왜 Long으로 했나요?
**답변**: [MEMBER_DOMAIN.md - id 필드 섹션](./auth/MEMBER_DOMAIN.md#1-id-long) 참고

- Wrapper 클래스로 null 체크 가능
- JPA/Hibernate 호환성
- 대용량 데이터 처리

## 🧪 테스트 코드

### 작성된 테스트
```
src/test/java/com/usinsa/backend/domain/auth/
├── service/
│   └── AuthServiceTest.java          # 단위 테스트: 로그인, 갱신, 로그아웃
├── token/
│   └── JwtTokenServiceTest.java      # 단위 테스트: 토큰 발급, Rotation
└── integration/
    └── AuthIntegrationTest.java      # 통합 테스트: 전체 플로우
```

### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# Auth 도메인 테스트만
./gradlew test --tests "com.usinsa.backend.domain.auth.*"

# 특정 테스트 클래스
./gradlew test --tests "AuthServiceTest"
```

## 📊 커버리지 목표

| 계층 | 목표 커버리지 | 현재 상태 |
|------|-------------|----------|
| Service | 80% 이상 | ✅ 달성 |
| Token | 90% 이상 | ✅ 달성 |
| Controller | 70% 이상 | 🔄 작업 중 |

## 🔄 문서 업데이트 가이드

### 언제 업데이트해야 하나요?
- 새로운 기능 추가 시
- 기존 로직 변경 시
- 설계 의사 결정이 있을 때
- 자주 묻는 질문이 생길 때

### 어떻게 업데이트하나요?
1. 해당 도메인의 md 파일 수정
2. 변경 사항을 커밋 메시지에 명확히 기록
3. PR 리뷰 시 문서 변경도 함께 확인

## 📝 문서 작성 원칙

1. **Why 중심**: "무엇을"이 아니라 "왜" 그렇게 했는지 설명
2. **구체적 예시**: 추상적 설명보다 코드와 시나리오 예시
3. **시각화**: 다이어그램과 테이블 적극 활용
4. **실용성**: 실제 개발에 도움이 되는 내용 중심

## 🚀 다음 작업

### 추가 예정 문서
- [ ] OAuth 2.0 소셜 로그인 가이드
- [ ] Redis 설정 및 장애 대응
- [ ] API 명세서 (Swagger 연동)
- [ ] 배포 가이드 (Docker, CI/CD)

### 개선 예정 항목
- [ ] 성능 최적화 문서
- [ ] 보안 체크리스트
- [ ] 트러블슈팅 가이드

## 📞 문의

문서에 대한 질문이나 개선 제안은:
- GitHub Issues에 등록
- PR로 직접 수정 제안

---

**최종 업데이트**: 2024년 11월 28일  
**문서 버전**: 1.0.0
