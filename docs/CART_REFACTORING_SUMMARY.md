# Cart 도메인 세션 기반 리팩토링 요약

## 변경 개요

기존의 회원 전용 장바구니 시스템을 세션 기반 비회원 장바구니를 지원하도록 리팩토링했습니다.

## 주요 변경 사항

### 1. 엔티티 수정 (Cart.java)

#### 변경 전
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id", nullable = false)
private Member member;

@Column(nullable = false)
private Integer count;
```

#### 변경 후
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id")  // nullable로 변경
private Member member;

@Column(name = "session_id", length = 255)  // 세션 ID 추가
private String sessionId;

@Column(nullable = false)
private Integer count;
```

#### 추가된 메서드
- `setMember(Member member)`: 병합 시 회원 설정
- `setSessionId(String sessionId)`: 세션 ID 설정
- `isGuestCart()`: 비회원 장바구니 여부 확인
- `isMemberCart()`: 회원 장바구니 여부 확인

### 2. DTO 수정 (CartDto.java)

#### 추가된 DTO
- `GuestCreateReq`: 비회원 장바구니 생성 요청 (memberId 불필요)
- `MergeRequest`: 병합 요청 (현재 미사용)

#### Response DTO 수정
```java
@Builder
public static class Response {
    private Long id;
    private Long productOptionId;
    private Long memberId;          // nullable
    private String sessionId;        // 추가
    private int count;
    private boolean isGuest;         // 추가
}
```

### 3. Repository 수정 (CartRepository.java)

#### 추가된 메서드
```java
// 세션 ID로 장바구니 조회
List<Cart> findBySessionId(String sessionId);

// 회원으로 장바구니 조회
List<Cart> findByMember(Member member);

// 세션 + 상품옵션으로 중복 체크
Optional<Cart> findBySessionIdAndProductOption(String sessionId, ProductOption productOption);

// 회원 + 상품옵션으로 중복 체크
Optional<Cart> findByMemberAndProductOption(Member member, ProductOption productOption);

// 세션 장바구니 전체 삭제
void deleteBySessionId(String sessionId);
```

### 4. Service 수정 (CartService.java)

#### 추가된 메서드
```java
// 비회원 장바구니 생성 (세션 기반)
CartDto.Response createGuestCart(CartDto.GuestCreateReq request, String sessionId)

// 세션 ID로 장바구니 조회
List<CartDto.Response> findBySessionId(String sessionId)

// 회원 ID로 장바구니 조회
List<CartDto.Response> findByMemberId(Long memberId)

// 비회원 장바구니를 회원 장바구니로 병합
List<CartDto.Response> mergeGuestCartToMember(String sessionId, Long memberId)

// 세션 장바구니 전체 삭제
void deleteGuestCart(String sessionId)
```

#### 수정된 메서드
- `create()`: 중복 상품 체크 및 수량 합산 로직 추가
- `toResDto()`: sessionId, isGuest 필드 추가

### 5. Controller 수정 (CartController.java)

#### 추가된 엔드포인트

| HTTP Method | Endpoint | 설명 |
|------------|----------|------|
| POST | `/carts/guest` | 비회원 장바구니 생성 |
| GET | `/carts/guest` | 비회원 장바구니 조회 |
| GET | `/carts/member/{memberId}` | 회원 장바구니 조회 |
| POST | `/carts/merge/{memberId}` | 장바구니 병합 (로그인 시) |
| DELETE | `/carts/guest` | 비회원 장바구니 전체 삭제 |

#### 세션 처리
- 모든 비회원 API에서 `HttpSession`을 주입받아 `session.getId()`로 세션 ID 획득
- 세션 ID를 서비스 계층으로 전달

### 6. 데이터베이스 마이그레이션

#### 스크립트 위치
`src/main/resources/db/migration/V1__add_session_id_to_cart.sql`

#### 주요 변경
- `member_id` 컬럼 nullable 변경
- `session_id` 컬럼 추가
- 성능 최적화를 위한 인덱스 추가:
  - `idx_cart_session_id`
  - `idx_cart_session_product` (복합)
  - `idx_cart_member_product` (복합)

### 7. 테스트 코드

#### 추가된 테스트 파일
1. `CartServiceSessionTest.java`: 세션 장바구니 단위 테스트
   - 비회원 장바구니 생성 테스트
   - 세션 장바구니 조회 테스트
   - 장바구니 병합 테스트
   - 중복 상품 처리 테스트

2. `CartControllerIntegrationTest.java`: 통합 테스트
   - API 엔드포인트 통합 테스트
   - 세션 처리 테스트
   - 병합 시나리오 테스트

## 동작 흐름

### 비회원 장바구니 사용 시나리오

1. **상품 추가**
   ```
   사용자 → POST /carts/guest → CartController
                                    ↓ session.getId()
                                CartService
                                    ↓ sessionId 저장
                                Cart 엔티티 (sessionId 포함)
   ```

2. **장바구니 조회**
   ```
   사용자 → GET /carts/guest → CartController
                                   ↓ session.getId()
                               CartService
                                   ↓ findBySessionId()
                               세션 장바구니 목록
   ```

### 로그인 시 병합 시나리오

1. **사용자가 비회원 상태에서 상품 A, B를 장바구니에 추가**
   - sessionId: "ABC123"
   - 장바구니: [상품A(2개), 상품B(1개)]

2. **로그인 수행**
   - memberId: 100

3. **병합 API 호출**
   ```
   POST /carts/merge/100
   ```

4. **병합 프로세스**
   ```
   CartService.mergeGuestCartToMember("ABC123", 100)
   
   ① 세션 장바구니 조회 (sessionId = "ABC123")
   ② 각 항목에 대해:
      - 회원 장바구니에 동일 상품이 있는지 확인
      - 있으면 → 수량 합산
      - 없으면 → sessionId 제거, memberId 설정
   ③ 세션 장바구니 삭제 (sessionId = "ABC123")
   ④ 회원 장바구니 반환
   ```

5. **결과**
   - 회원 장바구니: [상품A(2개), 상품B(1개)]
   - 세션 장바구니: []

## 장점

1. **사용자 경험 향상**
   - 로그인 없이도 장바구니 사용 가능
   - 로그인 후 장바구니 내용 보존

2. **전환율 증가**
   - 비회원 구매 프로세스 개선
   - 회원가입 장벽 감소

3. **데이터 일관성**
   - 중복 상품 자동 병합
   - 수량 자동 합산

4. **확장성**
   - 세션/회원 구분 명확
   - 추후 추가 기능 구현 용이

## 주의사항

### 프론트엔드 구현 시

1. **세션 쿠키 필수**
   ```javascript
   fetch('/carts/guest', {
     credentials: 'include'  // 필수!
   })
   ```

2. **로그인 시 병합 호출**
   ```javascript
   // 로그인 성공 후 반드시 병합 API 호출
   await fetch(`/carts/merge/${memberId}`, {
     method: 'POST',
     credentials: 'include'
   });
   ```

3. **장바구니 상태 관리**
   - 로그인 상태에 따라 API 엔드포인트 분기
   - 비회원: `/carts/guest`
   - 회원: `/carts/member/{memberId}`

### 백엔드 운영 시

1. **세션 타임아웃 설정**
   ```yaml
   server:
     servlet:
       session:
         timeout: 30m
   ```

2. **세션 스토리지 고려**
   - 트래픽이 많을 경우 Redis 등 외부 세션 스토리지 사용 권장

3. **주기적인 세션 장바구니 정리**
   - 오래된 세션 장바구니 삭제 배치 작업 필요

## 마이그레이션 체크리스트

- [ ] 데이터베이스 마이그레이션 스크립트 실행
- [ ] 기존 Cart 데이터 검증 (member_id NULL 확인)
- [ ] 프론트엔드 코드 수정
  - [ ] 비회원 장바구니 API 연동
  - [ ] 로그인 시 병합 로직 구현
  - [ ] credentials: 'include' 설정 확인
- [ ] CORS 설정 확인 (allowCredentials = true)
- [ ] 세션 설정 확인
- [ ] 통합 테스트 수행
- [ ] 성능 테스트 수행

## 참고 문서

- [세션 기반 비회원 장바구니 가이드](./CART_SESSION_GUIDE.md)
- [API 명세서](./CART_SESSION_GUIDE.md#api-명세)
- [프론트엔드 구현 예시](./CART_SESSION_GUIDE.md#프론트엔드-구현-예시)
