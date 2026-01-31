# 카카오페이 단건 결제 서비스

## 📁 구조

```
payment/
├── config/
│   ├── KakaoPayProperties.java         # 카카오페이 설정 정보
│   └── KakaoPayWebClientConfig.java    # WebClient 설정
├── controller/
│   └── KakaoPayController.java         # 결제 API 엔드포인트
├── dto/
│   └── KakaoPayDto.java                # 요청/응답 DTO
├── service/
│   ├── KakaoPayService.java            # 카카오페이 API 호출
│   └── PaymentService.java             # 결제 비즈니스 로직 (주문 통합)
└── store/
    └── PaymentTidStore.java            # Redis를 이용한 TID 저장소
```

## 🔧 설정

### application-secret.yml
```yaml
kakaopay:
  secretKey: "DEV359B6BC37D9018E0B63440D0CA7103E28AB2C"
  cid: TC0ONETIME

redis:
  password: usinsa
```

### Redis 연동
- **TID 저장**: `payment:tid:{orderId}` 형식으로 Redis에 저장
- **TTL**: 30분 (결제 준비 후 30분 이내에 승인 필요)
- **기존 Redis 설정**: 프로젝트에 이미 구성된 Redis 설정 활용

## 📌 API 엔드포인트

### 1. 결제 준비
**POST** `/api/v1/payments/kakao-pay/{orderId}/ready`

주문에 대한 카카오페이 결제를 준비합니다.

**요청 예시:**
```bash
POST /api/v1/payments/kakao-pay/1/ready
```

**응답 예시:**
```json
{
  "tid": "T1234567890123456789",
  "nextRedirectPcUrl": "https://mockup-pg-web.kakao.com/v1/...",
  "nextRedirectMobileUrl": "https://mockup-pg-web.kakao.com/v1/...",
  "createdAt": "2025-01-27T10:00:00"
}
```

**처리 과정:**
1. 주문 정보 조회 및 검증
2. 주문 상품 정보로 결제 정보 생성
3. 카카오페이 결제 준비 API 호출
4. TID를 Redis에 저장 (TTL: 30분)
5. 주문 상태를 `PAYMENT_READY`로 변경

### 2. 결제 승인
**POST** `/api/v1/payments/kakao-pay/{orderId}/approve?pgToken={pgToken}`

카카오페이 결제를 승인합니다.

**요청 예시:**
```bash
POST /api/v1/payments/kakao-pay/1/approve?pgToken=xxxxxxxxxx
```

**응답 예시:**
```json
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
  "itemName": "나이키 에어포스 외 2건",
  "approvedAt": "2025-01-27T10:05:00"
}
```

**처리 과정:**
1. Redis에서 TID 조회
2. 카카오페이 결제 승인 API 호출
3. 주문 상태를 `PAYMENT_COMPLETED`로 변경
4. Redis에서 TID 삭제

### 3. 결제 취소
**POST** `/api/v1/payments/kakao-pay/{orderId}/cancel`

카카오페이 결제를 취소합니다.

**요청 예시:**
```bash
POST /api/v1/payments/kakao-pay/1/cancel
```

**응답 예시:**
```json
{
  "aid": "A1234567890123456789",
  "tid": "T1234567890123456789",
  "cid": "TC0ONETIME",
  "status": "CANCEL_PAYMENT",
  "partnerOrderId": "1",
  "partnerUserId": "1",
  "paymentMethodType": "MONEY",
  "amount": {
    "total": 150000,
    "taxFree": 0,
    "vat": 13636
  },
  "canceledAt": "2025-01-27T10:10:00"
}
```

**처리 과정:**
1. Redis에서 TID 조회
2. 카카오페이 결제 취소 API 호출
3. 주문 상태를 `CANCELLED`로 변경
4. Redis에서 TID 삭제

## 🔄 결제 플로우

```
[프론트엔드]                    [백엔드]                      [카카오페이]
     |                            |                              |
     |--- 1. 결제 준비 요청 ------->|                              |
     |    POST /payments/.../ready |                              |
     |                            |--- 2. 결제 준비 API -------->|
     |                            |<--- TID 발급 ------------------|
     |                            |--- 3. TID 저장 (Redis) ----->|
     |                            |--- 4. 주문 상태 변경 -------->|
     |                            |   (CREATED -> PAYMENT_READY)  |
     |<--- redirect URL 반환 -------|                              |
     |                            |                              |
     |--- 5. 사용자를 카카오페이로 리다이렉트 ----------------->|
     |                            |                              |
     |<--- 6. 결제 진행 (사용자가 카카오페이 페이지에서 결제) ----|
     |                            |                              |
     |<--- 7. 성공 시 리다이렉트 (pg_token 포함) ----------------|
     | http://localhost:5173/payment/success?orderId=1&pg_token=xxx
     |                            |                              |
     |--- 8. 결제 승인 요청 ------->|                              |
     |    POST /payments/.../approve?pgToken=xxx                 |
     |                            |--- 9. TID 조회 (Redis) ----->|
     |                            |--- 10. 결제 승인 API ------->|
     |                            |<--- 승인 결과 -----------------|
     |                            |--- 11. 주문 상태 변경 ------>|
     |                            |   (PAYMENT_READY -> PAYMENT_COMPLETED)
     |                            |--- 12. TID 삭제 (Redis) ---->|
     |<--- 결제 완료 ---------------|                              |
```

## 🔗 주문 상태 관리

### OrderStatus
```java
public enum OrderStatus {
    CREATED,            // 주문 생성
    PAYMENT_READY,      // 결제 준비 완료
    PAYMENT_COMPLETED,  // 결제 완료
    CANCELLED           // 취소
}
```

### 상태 전이
- `CREATED` → `PAYMENT_READY`: 결제 준비 시
- `PAYMENT_READY` → `PAYMENT_COMPLETED`: 결제 승인 시
- `CREATED` / `PAYMENT_READY` → `CANCELLED`: 결제 취소 시

## 💡 사용 예시

### 프론트엔드 통합 예시

```javascript
// 1. 결제 준비
async function preparePayment(orderId) {
  const response = await fetch(`/api/v1/payments/kakao-pay/${orderId}/ready`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  
  const data = await response.json();
  
  // 카카오페이 결제 페이지로 리다이렉트
  window.location.href = data.nextRedirectPcUrl;
}

// 2. 결제 승인 (success 페이지에서 호출)
async function approvePayment(orderId, pgToken) {
  const response = await fetch(
    `/api/v1/payments/kakao-pay/${orderId}/approve?pgToken=${pgToken}`,
    { method: 'POST' }
  );
  
  const data = await response.json();
  console.log('결제 완료:', data);
  
  // 주문 완료 페이지로 이동
  window.location.href = `/orders/${orderId}/complete`;
}

// 3. URL 파라미터에서 pg_token 추출 후 승인 처리
// /payment/success?orderId=1&pg_token=xxx
const urlParams = new URLSearchParams(window.location.search);
const orderId = urlParams.get('orderId');
const pgToken = urlParams.get('pg_token');

if (orderId && pgToken) {
  approvePayment(orderId, pgToken);
}
```

### Redirect URL 설정
- **성공**: `http://localhost:5173/payment/success?orderId={orderId}`
- **취소**: `http://localhost:5173/payment/cancel?orderId={orderId}`
- **실패**: `http://localhost:5173/payment/fail?orderId={orderId}`

## 💡 주요 기능

### 1. Redis를 통한 TID 관리
- 결제 준비 시 TID를 Redis에 저장 (TTL: 30분)
- 결제 승인/취소 시 Redis에서 TID 조회
- 결제 완료/취소 후 TID 삭제

### 2. Order 서비스 통합
- 결제 준비 시 주문 정보로 자동 생성
- 주문 상품 정보로 상품명, 수량, 금액 계산
- **금액 계산**: ProductOption은 가격 정보가 없으므로 Product.price 사용
- 결제 상태에 따른 주문 상태 자동 업데이트

### 3. 에러 처리
- 주문을 찾을 수 없는 경우: `ORDER_NOT_FOUND`
- TID를 찾을 수 없는 경우: `PAYMENT_TID_NOT_FOUND`
- 이미 결제 완료된 경우: `PAYMENT_ALREADY_COMPLETED`
- 결제 실패: `PAYMENT_FAILED`
- 결제 취소 실패: `PAYMENT_CANCEL_FAILED`

## 🔍 테스트 방법

### 1. 주문 생성
먼저 주문을 생성합니다.
```bash
POST /api/v1/orders
{
  "memberId": 1,
  "receiverAddress": "서울시 강남구",
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678"
}
```

### 2. 결제 준비
생성된 주문 ID로 결제를 준비합니다.
```bash
POST /api/v1/payments/kakao-pay/1/ready
```

### 3. 결제 진행
응답받은 `nextRedirectPcUrl`로 접속하여 결제를 진행합니다.

### 4. 결제 승인
성공 시 리다이렉트된 URL에서 `pg_token`을 추출하여 승인 요청합니다.
```bash
POST /api/v1/payments/kakao-pay/1/approve?pgToken=xxxxxxxxxx
```

## ⚠️ 주의사항

1. **Redis 필수**: TID 저장을 위해 Redis가 실행 중이어야 합니다.
2. **TTL 관리**: TID는 30분 후 자동 삭제되므로, 결제는 30분 이내에 완료해야 합니다.
3. **주문 상품 필수**: 결제 준비 시 주문에 최소 1개 이상의 상품이 있어야 합니다.
4. **부가세 계산**: 총액의 1/11 (10%)로 자동 계산됩니다.
5. **프론트엔드 URL**: 현재 `http://localhost:5173`으로 설정되어 있으며, 운영 환경에서는 변경 필요합니다.

## 🔗 참고

- [카카오페이 개발자센터](https://developers.kakaopay.com/)
- [카카오페이 단건결제 API 문서](https://developers.kakaopay.com/docs/payment/online)
