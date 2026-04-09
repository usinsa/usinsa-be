# 카카오페이 결제 통합 가이드

## 🎯 개요

usinsa-be 프로젝트에 카카오페이 단건 결제 서비스가 통합되었습니다.
주문 생성부터 결제 완료까지의 전체 플로우를 지원합니다.

## ✅ 구현 완료 사항

### 1. Redis 통합
- ✅ 기존 Redis 설정 활용
- ✅ `PaymentTidStore` 구현 (TID 저장/조회/삭제)
- ✅ TID TTL 30분 설정
- ✅ Key 형식: `payment:tid:{orderId}`

### 2. Order 서비스 통합
- ✅ `OrderStatus` 확장 (PAYMENT_READY, PAYMENT_COMPLETED 추가)
- ✅ 결제 준비/승인/취소 시 주문 상태 자동 업데이트
- ✅ 주문 상품 정보로 결제 정보 자동 생성
- ✅ 상품명, 수량, 금액 자동 계산
- ✅ **금액 계산 로직**: ProductOption은 가격 없음 → Product.price 사용

### 3. 프론트엔드 URL 설정
- ✅ Redirect URL: `http://localhost:5173`
- ✅ 성공: `/payment/success?orderId={orderId}`
- ✅ 취소: `/payment/cancel?orderId={orderId}`
- ✅ 실패: `/payment/fail?orderId={orderId}`

### 4. 에러 처리
- ✅ `ErrorCode`에 결제 관련 에러 추가
- ✅ 결제 실패 시 예외 처리
- ✅ TID 조회 실패 시 예외 처리

## 🚀 빠른 시작

### 1. Redis 실행
```bash
# Docker로 Redis 실행
docker-compose up -d redis

# 또는 로컬 Redis 실행
redis-server
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. Swagger UI 접속
```
http://localhost:8080/swagger-ui/index.html
```

## 📋 전체 플로우 테스트

### Step 1: 주문 생성
```bash
POST http://localhost:8080/api/v1/orders
Content-Type: application/json

{
  "memberId": 1,
  "receiverAddress": "서울시 강남구 테헤란로 123",
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678"
}

# 응답 예시
{
  "id": 1,
  "memberId": 1,
  "receiverAddress": "서울시 강남구 테헤란로 123",
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678",
  "status": "CREATED"
}
```

### Step 2: 주문에 상품 추가
```bash
POST http://localhost:8080/api/v1/ordered-products
Content-Type: application/json

{
  "orderId": 1,
  "productOptionId": 1,
  "quantity": 2
}
```

### Step 3: 결제 준비
```bash
POST http://localhost:8080/api/v1/payments/kakao-pay/1/ready

# 응답 예시
{
  "tid": "T1234567890123456789",
  "nextRedirectPcUrl": "https://mockup-pg-web.kakao.com/v1/...",
  "nextRedirectMobileUrl": "https://mockup-pg-web.kakao.com/v1/...",
  "createdAt": "2025-01-27T10:00:00"
}
```

**백엔드 처리 내용:**
- 주문 상태: `CREATED` → `PAYMENT_READY`
- Redis에 TID 저장: `payment:tid:1` = `T1234567890123456789`

### Step 4: 카카오페이 결제 진행
프론트엔드에서 `nextRedirectPcUrl`로 리다이렉트하여 사용자가 결제를 진행합니다.

결제 성공 시 카카오페이가 다음 URL로 리다이렉트합니다:
```
http://localhost:5173/payment/success?orderId=1&pg_token=xxxxxxxxxx
```

### Step 5: 결제 승인
프론트엔드에서 `pg_token`을 받아 결제 승인을 요청합니다:
```bash
POST http://localhost:8080/api/v1/payments/kakao-pay/1/approve?pgToken=xxxxxxxxxx

# 응답 예시
{
  "aid": "A1234567890123456789",
  "tid": "T1234567890123456789",
  "cid": "TC0ONETIME",
  "partnerOrderId": "1",
  "partnerUserId": "1",
  "paymentMethodType": "MONEY",
  "amount": {
    "total": 150000,
    "taxFree": 0,
    "vat": 13636,
    "point": 0,
    "discount": 0
  },
  "itemName": "나이키 에어포스 외 1건",
  "approvedAt": "2025-01-27T10:05:00"
}
```

**백엔드 처리 내용:**
- 주문 상태: `PAYMENT_READY` → `PAYMENT_COMPLETED`
- Redis에서 TID 삭제

### Step 6 (선택): 결제 취소
```bash
POST http://localhost:8080/api/v1/payments/kakao-pay/1/cancel

# 응답 예시
{
  "aid": "A1234567890123456789",
  "tid": "T1234567890123456789",
  "status": "CANCEL_PAYMENT",
  "amount": {
    "total": 150000,
    "taxFree": 0,
    "vat": 13636
  },
  "canceledAt": "2025-01-27T10:10:00"
}
```

**백엔드 처리 내용:**
- 주문 상태: `PAYMENT_READY` → `CANCELLED`
- Redis에서 TID 삭제

## 🔍 Redis 데이터 확인

```bash
# Redis CLI 접속
redis-cli

# 비밀번호 입력
AUTH usinsa

# TID 확인
GET payment:tid:1

# 모든 결제 TID 확인
KEYS payment:tid:*

# TTL 확인
TTL payment:tid:1
```

## 📊 주문 상태 확인

```bash
# 주문 조회
GET http://localhost:8080/api/v1/orders/1

# 응답에서 status 확인
{
  "id": 1,
  "status": "PAYMENT_COMPLETED",  # 결제 완료
  ...
}
```

## 🎨 프론트엔드 통합 예시

### React 예시
```javascript
// PaymentPage.jsx
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  useEffect(() => {
    const orderId = searchParams.get('orderId');
    const pgToken = searchParams.get('pg_token');
    
    if (orderId && pgToken) {
      approvePayment(orderId, pgToken);
    }
  }, []);
  
  const approvePayment = async (orderId, pgToken) => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/payments/kakao-pay/${orderId}/approve?pgToken=${pgToken}`,
        { method: 'POST' }
      );
      
      if (response.ok) {
        const data = await response.json();
        console.log('결제 완료:', data);
        navigate(`/orders/${orderId}/complete`);
      } else {
        throw new Error('결제 승인 실패');
      }
    } catch (error) {
      console.error('결제 오류:', error);
      navigate('/payment/fail');
    }
  };
  
  return <div>결제 처리 중...</div>;
}

// OrderPage.jsx
function OrderPage() {
  const handlePayment = async (orderId) => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/payments/kakao-pay/${orderId}/ready`,
        { method: 'POST' }
      );
      
      const data = await response.json();
      
      // 카카오페이 결제 페이지로 리다이렉트
      window.location.href = data.nextRedirectPcUrl;
    } catch (error) {
      console.error('결제 준비 실패:', error);
    }
  };
  
  return (
    <button onClick={() => handlePayment(1)}>
      카카오페이로 결제하기
    </button>
  );
}
```

## 🐛 트러블슈팅

### 1. Redis 연결 오류
**증상:** `Could not connect to Redis`
**해결:**
```bash
# Redis가 실행 중인지 확인
docker ps | grep redis

# Redis 실행
docker-compose up -d redis
```

### 2. TID를 찾을 수 없음
**증상:** `PAYMENT_TID_NOT_FOUND` 에러
**원인:** 
- 결제 준비 후 30분이 지나 TTL로 인해 TID가 삭제됨
- 결제 준비를 하지 않고 승인을 시도함

**해결:**
```bash
# Redis에서 TID 확인
redis-cli
AUTH usinsa
GET payment:tid:1

# TID가 없으면 결제 준비부터 다시 진행
```

### 3. 주문에 상품이 없음
**증상:** `INVALID_INPUT_VALUE` 에러
**원인:** 결제 준비 시 주문에 상품이 없음

**해결:**
```bash
# 주문에 상품 추가
POST /api/v1/ordered-products
{
  "orderId": 1,
  "productOptionId": 1,
  "quantity": 1
}
```

### 4. 이미 결제 완료된 주문
**증상:** `PAYMENT_ALREADY_COMPLETED` 에러
**원인:** 이미 결제 준비가 완료되어 Redis에 TID가 존재함

**해결:**
```bash
# Redis에서 TID 삭제
redis-cli
AUTH usinsa
DEL payment:tid:1

# 또는 새로운 주문 생성
```

## 📝 체크리스트

결제 기능을 사용하기 전에 다음을 확인하세요:

- [ ] Redis가 실행 중입니다
- [ ] 주문이 생성되었습니다
- [ ] 주문에 상품이 추가되었습니다
- [ ] 프론트엔드 redirect URL이 설정되었습니다
- [ ] 카카오페이 테스트 CID/SECRET_KEY가 설정되었습니다

## 🔗 관련 문서

- [Payment Domain README](../src/main/java/com/usinsa/backend/domain/payment/README.md)
- [카카오페이 개발자센터](https://developers.kakaopay.com/)
