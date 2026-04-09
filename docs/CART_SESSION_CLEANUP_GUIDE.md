# 비회원 장바구니 세션 정리 가이드

## 개요

비회원이 장바구니에 상품을 담은 후 로그인하지 않고 서비스를 떠날 경우, DB에 의미 없는 장바구니 데이터가 계속 쌓이는 문제를 해결하기 위한 자동 정리 메커니즘입니다.

## 문제점

### 기존 구조의 문제
```
1. 비회원이 장바구니에 상품 추가
   → sessionId로 Cart 엔티티 생성

2-A. 비회원이 로그인
   → mergeGuestCartToMember 호출
   → 세션 장바구니가 회원 장바구니로 병합/삭제 ✅

2-B. 비회원이 로그인하지 않고 종료
   → 세션 장바구니가 DB에 영구 보관 ❌
   → 시간이 지나면서 불필요한 데이터 축적
```

### 예상 시나리오
- 하루 방문자 1000명 중 30%가 비회원으로 장바구니 사용
- 그 중 70%가 로그인하지 않고 종료
- **하루 210개의 불필요한 장바구니 레코드 생성**
- 1년이면 약 76,650개의 의미 없는 데이터 축적

## 해결 방법

### 세션 리스너를 통한 자동 정리

HTTP 세션의 생명주기를 감지하여 세션 만료 시 해당 세션의 비회원 장바구니를 자동으로 삭제합니다.

```
1. 비회원이 장바구니에 상품 추가
   → sessionId로 Cart 생성

2. 세션 활성 상태 유지 (기본 30분)
   
3-A. 비회원이 로그인
   → 장바구니 병합 후 세션 장바구니 삭제 ✅

3-B. 30분간 활동 없음 → 세션 만료
   → SessionCleanupListener.sessionDestroyed() 호출
   → CartService.deleteGuestCartBySessionId() 실행
   → 해당 세션의 모든 장바구니 자동 삭제 ✅
```

## 구현 구조

### 1. SessionCleanupListener (세션 리스너)

```java
@Component
public class SessionCleanupListener implements HttpSessionListener {
    
    private final CartService cartService;
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        
        // 해당 세션의 비회원 장바구니 삭제
        int deletedCount = cartService.deleteGuestCartBySessionId(sessionId);
        
        log.info("세션 만료로 비회원 장바구니 정리: {} 항목 삭제", deletedCount);
    }
}
```

**동작 시점:**
- 사용자가 명시적으로 세션 종료 (`session.invalidate()`)
- 세션 타임아웃 (설정된 시간 동안 요청 없음)
- 서버 재시작 시 (모든 활성 세션 종료)

### 2. CartService.deleteGuestCartBySessionId()

```java
@Transactional
public int deleteGuestCartBySessionId(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
        return 0;  // 안전한 처리
    }
    
    List<Cart> guestCarts = cartRepository.findBySessionId(sessionId);
    int count = guestCarts.size();
    
    if (count > 0) {
        cartRepository.deleteBySessionId(sessionId);
    }
    
    return count;
}
```

**특징:**
- 예외 발생 시에도 세션 종료는 정상 진행 (try-catch 처리)
- 삭제된 항목 수 반환 (로깅 및 모니터링용)
- null/빈 문자열에 대한 안전한 처리

### 3. SessionConfig (세션 설정)

```java
@Configuration
public class SessionConfig {
    
    @Bean
    public ServletListenerRegistrationBean<SessionCleanupListener> sessionCleanupListener(
            SessionCleanupListener listener) {
        return new ServletListenerRegistrationBean<>(listener);
    }
}
```

### 4. application.yml (세션 타임아웃 설정)

```yaml
spring:
  session:
    timeout: 30m  # 30분간 요청 없으면 세션 만료
```

## 동작 흐름

### 시나리오 1: 비회원이 로그인하지 않고 종료

```
시간축 |-----|-----|-----|-----|-----|-----|
      0m    10m   20m   30m   40m   50m   60m

0m:  비회원 A가 상품 추가
     → Cart(sessionId=ABC, productOption=1, count=2) 생성
     
10m: 비회원 A가 또 다른 상품 추가
     → Cart(sessionId=ABC, productOption=5, count=1) 생성
     → 세션 타임아웃 카운터 리셋 (마지막 요청 시간 갱신)
     
40m: 마지막 요청 후 30분 경과 → 세션 만료
     → SessionCleanupListener.sessionDestroyed() 호출
     → cartService.deleteGuestCartBySessionId("ABC")
     → 2개의 Cart 레코드 삭제 ✅
```

### 시나리오 2: 비회원이 로그인 (병합)

```
0m:  비회원 B가 상품 추가
     → Cart(sessionId=XYZ, member=null, productOption=3, count=1) 생성
     
15m: 비회원 B가 로그인 (memberId=100)
     → cartService.mergeGuestCartToMember("XYZ", 100)
     → Cart 업데이트: sessionId=null, member=100
     → 세션 장바구니는 이미 회원 장바구니로 전환됨
     
45m: 세션 만료
     → SessionCleanupListener.sessionDestroyed() 호출
     → cartService.deleteGuestCartBySessionId("XYZ")
     → findBySessionId("XYZ") 결과: 빈 리스트 (이미 병합됨)
     → 삭제할 항목 없음, 회원 장바구니는 유지 ✅
```

### 시나리오 3: 회원이 직접 장바구니 사용

```
0m:  회원 C(memberId=200)가 로그인 상태로 상품 추가
     → Cart(member=200, sessionId=null, productOption=7, count=3) 생성
     
45m: 세션 만료
     → SessionCleanupListener.sessionDestroyed() 호출
     → cartService.deleteGuestCartBySessionId("DEF")
     → findBySessionId("DEF") 결과: 빈 리스트 (애초에 sessionId로 생성 안 됨)
     → 회원 장바구니는 member 기반이므로 영향 없음 ✅
```

## 장점

### 1. 자동 데이터 정리
- 비회원 장바구니가 자동으로 정리되어 DB 용량 절약
- 불필요한 레코드 축적 방지

### 2. 성능 개선
- 장바구니 테이블의 불필요한 레코드 감소
- 인덱스 효율성 향상 (sessionId 인덱스)

### 3. 사용자 경험 개선
- 오래된 비회원 장바구니가 갑자기 나타나는 현상 방지
- 매번 새로운 세션으로 깨끗한 장바구니 시작

### 4. 안전한 구현
- 회원 장바구니는 절대 삭제되지 않음
- 병합된 장바구니도 안전하게 유지
- 예외 발생 시에도 세션 종료는 정상 진행

## 모니터링

### 로그 예시

```log
# 정상적인 세션 정리
2024-12-22 14:32:15 INFO  SessionCleanupListener - 세션 종료 감지 - SessionId: 1A2B3C4D, 비회원 장바구니 정리 시작
2024-12-22 14:32:15 INFO  CartService - 세션 만료로 인한 비회원 장바구니 삭제 - SessionId: 1A2B3C4D, 삭제 항목 수: 3
2024-12-22 14:32:15 INFO  SessionCleanupListener - 비회원 장바구니 정리 완료 - SessionId: 1A2B3C4D, 삭제된 항목 수: 3

# 정리할 장바구니가 없는 경우 (로그인 후 병합됨)
2024-12-22 15:10:22 INFO  SessionCleanupListener - 세션 종료 감지 - SessionId: 5E6F7G8H, 비회원 장바구니 정리 시작
2024-12-22 15:10:22 DEBUG SessionCleanupListener - 정리할 비회원 장바구니 없음 - SessionId: 5E6F7G8H

# 오류 발생 시 (세션 종료는 계속 진행)
2024-12-22 16:20:30 ERROR SessionCleanupListener - 비회원 장바구니 정리 중 오류 발생 - SessionId: 9I0J1K2L, Error: Database connection timeout
```

### 메트릭 수집 (선택사항)

```java
@Component
public class CartMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordGuestCartCleanup(int deletedCount) {
        meterRegistry.counter("cart.guest.cleanup", 
            "count", String.valueOf(deletedCount)).increment();
    }
}
```

## 설정 가이드

### 세션 타임아웃 조정

```yaml
# 개발 환경: 빠른 테스트를 위해 짧게
spring:
  session:
    timeout: 5m

# 스테이징 환경: 실제 환경과 유사하게
spring:
  session:
    timeout: 30m

# 프로덕션 환경: 사용자 편의성 고려
spring:
  session:
    timeout: 2h
```

**권장 설정:**
- 개발: 5~10분 (테스트 용이)
- 프로덕션: 30분~2시간 (업종 특성에 따라 조정)

### Redis를 통한 세션 공유 (선택사항)

다중 서버 환경에서는 Redis를 통해 세션을 공유할 수 있습니다:

```yaml
spring:
  session:
    store-type: redis
    timeout: 30m
```

```java
@EnableRedisHttpSession
public class SessionConfig {
    // ...
}
```

## 주의사항

### 1. 세션 타임아웃 설정
- 너무 짧으면: 사용자가 상품을 둘러보는 중 장바구니가 사라질 수 있음
- 너무 길면: 불필요한 데이터가 오래 유지됨
- **권장: 30분 ~ 1시간**

### 2. 병합 로직과의 관계
- 로그인 시 `mergeGuestCartToMember`가 먼저 호출되어야 함
- 병합 후에는 sessionId가 null로 변경되므로 세션 만료 시 영향 없음

### 3. 서버 재시작
- 서버 재시작 시 모든 활성 세션이 종료됨
- 이때 모든 비회원 장바구니가 정리될 수 있음
- **대응:** Redis 세션 저장소 사용 (재시작 시에도 세션 유지)

### 4. 트랜잭션 관리
- `deleteGuestCartBySessionId`는 `@Transactional` 적용
- 예외 발생 시 롤백되지만 세션 종료는 계속 진행

## 테스트

테스트 코드를 통해 다음 시나리오를 검증합니다:

1. ✅ 세션 만료 시 비회원 장바구니 자동 삭제
2. ✅ 여러 비회원 장바구니 항목 동시 삭제
3. ✅ 로그인 후 병합된 장바구니는 세션 만료 시 삭제되지 않음
4. ✅ 유효하지 않은 세션 ID로 삭제 시도
5. ✅ 존재하지 않는 세션 ID로 삭제 시도
6. ✅ 회원 장바구니는 세션 만료의 영향을 받지 않음

```bash
# 테스트 실행
./gradlew test --tests CartSessionCleanupTest
```

## 대안 방안

### 1. 스케줄러를 통한 배치 정리

```java
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
public void cleanupOldGuestCarts() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
    cartRepository.deleteOldGuestCarts(cutoff);
}
```

**장단점:**
- 장점: 세션 리스너보다 간단, 오래된 데이터 일괄 정리
- 단점: 실시간 정리 안 됨, 최대 7일간 데이터 보관

### 2. TTL(Time To Live) 인덱스 (MongoDB 사용 시)

```javascript
db.carts.createIndex(
  { "createdAt": 1 }, 
  { expireAfterSeconds: 1800 }  // 30분
)
```

**장단점:**
- 장점: DB 레벨에서 자동 정리, 애플리케이션 로직 불필요
- 단점: MongoDB에서만 사용 가능

## 결론

세션 리스너를 통한 비회원 장바구니 자동 정리는:
- ✅ 실시간으로 불필요한 데이터 정리
- ✅ 회원 데이터는 절대 삭제하지 않음
- ✅ 로그인 후 병합된 데이터도 안전하게 보호
- ✅ 예외 안전성 보장

비회원 쇼핑 경험을 유지하면서도 DB를 깨끗하게 관리할 수 있는 효율적인 방법입니다.
