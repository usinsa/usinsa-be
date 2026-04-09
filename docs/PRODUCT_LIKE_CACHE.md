# ProductLike Redis Cache 구현 문서

## 개요

ProductLike 기능에 Redis Cache Aside Pattern을 적용하여 읽기 성능을 최적화하고, 중복 체크 및 좋아요 카운팅의 효율성을 향상시킵니다.

## 캐싱 전략 패턴 사용 이유
DB에서 데이터 조회, 작성을 전부 처리할 경우 데이터 정합성 문제가 발생하지 않습니다.
그러나 시스템의 성능 향상을 위해 캐시를 사용할 경우 또 다른 데이터 저장소 사용으로 인해 같은 종류의 데이터라도 저장된 값이 서로 다를 수 있는 현상이 발생합니다.
이를 위해 캐시와 DB간의 불일치 문제를 극복하면서 빠른 성능을 잃지 않기 위해 전략 패턴을 도입했습니다.

## 아키텍처

### Cache Aside Pattern

Cache Aside Pattern은 애플리케이션이 캐시를 직접 관리하는 패턴입니다.

```
1. 읽기 (Read-Through)
   Client → Application → Cache 조회
                       ↓ (Cache Miss)
                    Database 조회
                       ↓
                    Cache 저장
                       ↓
                    Client 응답

2. 쓰기 (Write-Through)
   Client → Application → Database 저장
                       ↓
                    Cache 업데이트/무효화
                       ↓
                    Client 응답
```

## 주요 구성 요소

### 1. ProductLikeCacheService

Redis 캐시 레이어를 담당하는 서비스

**캐시 키 구조:**
```
product:like:count:{productId}      - 상품별 좋아요 개수
product:like:member:{memberId}      - 회원별 좋아요한 상품 Set
product:like:likers:{productId}     - 상품별 좋아요한 회원 Set
```

**TTL 설정:**
- 좋아요 개수: 24시간
- 회원 좋아요 목록: 12시간
- 상품 좋아요한 회원 목록: 12시간

**주요 메서드:**
- `getLikeCount(productId)` - 좋아요 개수 조회
- `setLikeCount(productId, count)` - 좋아요 개수 저장
- `incrementLikeCount(productId)` - 좋아요 개수 증가
- `decrementLikeCount(productId)` - 좋아요 개수 감소
- `isMemberLikedProduct(memberId, productId)` - 좋아요 여부 확인
- `addMemberLike(memberId, productId)` - 좋아요 추가
- `removeMemberLike(memberId, productId)` - 좋아요 제거

### 2. ProductLikeService

비즈니스 로직과 캐시 통합을 담당

**캐시 적용 메서드:**
- `addLike()` - 좋아요 추가 (중복 체크 캐시 활용)
- `removeLike()` - 좋아요 취소 (캐시 업데이트)
- `getLikeStatus()` - 좋아요 상태 조회 (캐시 우선)
- `getLikeCount()` - 좋아요 개수 조회 (캐시 우선)

### 3. ProductService

상품 조회 시 캐시된 좋아요 개수 활용

## 성능 최적화 전략

### 1. 읽기 성능 최적화

**좋아요 개수 조회:**
```java
// Before: 매번 DB COUNT 쿼리
int count = productLikeRepository.countByProductId(productId);

// After: 캐시 우선 조회
Integer cached = cacheService.getLikeCount(productId);
if (cached != null) return cached;  // Cache Hit
int count = productLikeRepository.countByProductId(productId);
cacheService.setLikeCount(productId, count);  // Cache Miss
return count;
```

**예상 성능 향상:**
- DB 쿼리 → Redis 조회: 약 10-100배 빠름
- 응답 시간: 50-100ms → 1-5ms

### 2. 중복 체크 최적화

**좋아요 중복 체크:**
```java
// Before: 매번 DB EXISTS 쿼리
boolean exists = productLikeRepository.existsByMemberIdAndProductId(...);

// After: 캐시 우선 조회
Boolean cached = cacheService.isMemberLikedProduct(memberId, productId);
if (cached != null) return cached;  // Cache Hit
// Cache Miss: 회원의 전체 좋아요 목록 조회 및 캐시 저장 (워밍업)
```

**예상 성능 향상:**
- 첫 조회: DB 쿼리 1회 + 캐시 저장
- 이후 조회: 캐시에서 O(1) 조회

### 3. 좋아요 카운팅 최적화

**좋아요 추가/취소:**
```java
// 좋아요 추가
cacheService.incrementLikeCount(productId);  // Redis INCR (원자적 연산)
cacheService.addMemberLike(memberId, productId);  // Redis SADD

// 좋아요 취소
cacheService.decrementLikeCount(productId);  // Redis DECR (원자적 연산)
cacheService.removeMemberLike(memberId, productId);  // Redis SREM
```

**장점:**
- 원자적 연산으로 동시성 문제 해결
- DB 부하 감소

## 캐시 일관성 관리

### 1. Write-Through 전략

좋아요 추가/취소 시 즉시 캐시 업데이트
```java
// DB 저장
productLikeRepository.save(productLike);

// 캐시 업데이트
cacheService.addMemberLike(memberId, productId);
cacheService.incrementLikeCount(productId);
```

### 2. 캐시 무효화

데이터 정합성이 의심될 때 캐시 무효화
```java
// 상품 삭제 시
cacheService.invalidateProductCache(productId);

// 회원 탈퇴 시 (필요 시)
cacheService.invalidateMemberCache(memberId);
```

### 3. Cache Warming

자주 조회되는 데이터 미리 캐싱
```java
// 로그인 시 회원의 좋아요 목록 캐싱
productLikeService.warmupMemberLikeCache(memberId);
```

## 모니터링 및 관리

### Admin API

캐시 관리를 위한 관리자 API 제공

```bash
# 회원 캐시 워밍업
POST /api/v1/admin/product-like-cache/warmup/member/{memberId}

# 상품 캐시 무효화
DELETE /api/v1/admin/product-like-cache/invalidate/product/{productId}

# 회원 캐시 무효화
DELETE /api/v1/admin/product-like-cache/invalidate/member/{memberId}
```

### 로깅

모든 캐시 작업은 로그로 기록됨
```
[DEBUG] 좋아요 개수 캐시 히트: productId=1, count=42
[DEBUG] 좋아요 개수 캐시 미스 - DB 조회: productId=2, count=15
[INFO] 좋아요 추가: memberId=10, productId=1
```

## 장애 대응

### 1. Redis 장애 시

Redis가 다운되어도 DB 조회로 폴백
```java
try {
    return cacheService.getLikeCount(productId);
} catch (Exception e) {
    log.error("Redis 조회 실패, DB 폴백: {}", e.getMessage());
    return productLikeRepository.countByProductId(productId);
}
```

### 2. 캐시 불일치 시

관리자 API로 캐시 무효화 후 재생성

## 성능 지표

### 예상 성능 개선

| 작업 | Before (DB) | After (Redis) | 개선율 |
|------|-------------|---------------|--------|
| 좋아요 개수 조회 | 50-100ms | 1-5ms | 90-98% |
| 중복 체크 | 20-50ms | 1-3ms | 94-98% |
| 좋아요 상태 조회 | 30-70ms | 2-5ms | 93-97% |
| 상품 목록 조회 (100개) | 5-10s | 0.5-1s | 80-90% |

### Redis 메모리 사용량 추정

- 회원당 좋아요 목록: 약 1KB (100개 상품 기준)
- 상품당 좋아요 개수: 약 50B
- 10만 회원, 1만 상품 기준: 약 100MB

## 결론

Cache Aside Pattern을 적용한 ProductLike 기능은:
- **읽기 성능 90% 이상 향상**
- **DB 부하 대폭 감소**
- **동시성 문제 해결** (Redis 원자적 연산)
- **확장성 확보** (캐시 레이어 분리)

을 달성하여 대용량 트래픽 환경에서도 안정적인 서비스 제공이 가능합니다.
