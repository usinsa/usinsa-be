# 세션 기반 비회원 장바구니 가이드

## 개요

이 문서는 비회원(세션 기반) 장바구니 기능과 로그인 시 장바구니 병합 기능에 대한 가이드입니다.

## 주요 기능

### 1. 비회원 장바구니 (세션 기반)
- 로그인하지 않은 사용자도 장바구니에 상품을 담을 수 있습니다
- 세션 ID를 기반으로 장바구니를 관리합니다
- 브라우저를 닫지 않는 한 장바구니 정보가 유지됩니다

### 2. 로그인 시 장바구니 병합
- 비회원 상태에서 담은 상품을 로그인 후 회원 장바구니로 자동 병합합니다
- 동일 상품이 있는 경우 수량을 합산합니다
- 병합 후 세션 장바구니는 자동으로 삭제됩니다

## API 명세

### 비회원 장바구니 생성
```http
POST /carts/guest
Content-Type: application/json

{
  "productOptionId": 1,
  "count": 2
}
```

**응답:**
```json
{
  "id": 1,
  "productOptionId": 1,
  "memberId": null,
  "sessionId": "A1B2C3D4E5F6G7H8",
  "count": 2,
  "isGuest": true
}
```

### 비회원 장바구니 조회
```http
GET /carts/guest
```

**응답:**
```json
[
  {
    "id": 1,
    "productOptionId": 1,
    "memberId": null,
    "sessionId": "A1B2C3D4E5F6G7H8",
    "count": 2,
    "isGuest": true
  }
]
```

### 회원 장바구니 생성 (기존)
```http
POST /carts
Content-Type: application/json

{
  "productOptionId": 1,
  "memberId": 100,
  "count": 1
}
```

### 회원 장바구니 조회
```http
GET /carts/member/{memberId}
```

### 장바구니 병합 (로그인 시 호출)
```http
POST /carts/merge/{memberId}
```

이 API는 사용자가 로그인할 때 호출하여 비회원 장바구니를 회원 장바구니로 병합합니다.

**응답:**
```json
[
  {
    "id": 1,
    "productOptionId": 1,
    "memberId": 100,
    "sessionId": null,
    "count": 2,
    "isGuest": false
  }
]
```

### 장바구니 수량 수정
```http
PUT /carts/{id}
Content-Type: application/json

{
  "count": 5
}
```

### 장바구니 삭제
```http
DELETE /carts/{id}
```

### 비회원 장바구니 전체 삭제
```http
DELETE /carts/guest
```

## 프론트엔드 구현 예시

### 1. 비회원 상품 담기
```javascript
async function addToGuestCart(productOptionId, count) {
  const response = await fetch('/carts/guest', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // 세션 쿠키 포함 필수
    body: JSON.stringify({
      productOptionId,
      count
    })
  });
  
  return await response.json();
}
```

### 2. 비회원 장바구니 조회
```javascript
async function getGuestCart() {
  const response = await fetch('/carts/guest', {
    credentials: 'include' // 세션 쿠키 포함 필수
  });
  
  return await response.json();
}
```

### 3. 로그인 시 장바구니 병합
```javascript
async function loginAndMergeCart(credentials) {
  // 1. 로그인 처리
  const loginResponse = await fetch('/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify(credentials)
  });
  
  const loginData = await loginResponse.json();
  const memberId = loginData.memberId;
  
  // 2. 장바구니 병합
  const mergeResponse = await fetch(`/carts/merge/${memberId}`, {
    method: 'POST',
    credentials: 'include'
  });
  
  const mergedCart = await mergeResponse.json();
  console.log('병합된 장바구니:', mergedCart);
  
  return mergedCart;
}
```

### 4. React 예시
```jsx
import { useEffect, useState } from 'react';

function ShoppingCart() {
  const [cartItems, setCartItems] = useState([]);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [memberId, setMemberId] = useState(null);

  // 장바구니 조회
  useEffect(() => {
    const fetchCart = async () => {
      const url = isLoggedIn 
        ? `/carts/member/${memberId}`
        : '/carts/guest';
      
      const response = await fetch(url, {
        credentials: 'include'
      });
      const data = await response.json();
      setCartItems(data);
    };

    fetchCart();
  }, [isLoggedIn, memberId]);

  // 상품 추가
  const addToCart = async (productOptionId, count) => {
    const url = isLoggedIn ? '/carts' : '/carts/guest';
    const body = isLoggedIn 
      ? { productOptionId, memberId, count }
      : { productOptionId, count };

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(body)
    });

    const newItem = await response.json();
    setCartItems([...cartItems, newItem]);
  };

  // 로그인 후 병합
  const handleLogin = async (credentials) => {
    const loginResponse = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(credentials)
    });

    const loginData = await loginResponse.json();
    
    // 장바구니 병합
    const mergeResponse = await fetch(`/carts/merge/${loginData.memberId}`, {
      method: 'POST',
      credentials: 'include'
    });

    const mergedCart = await mergeResponse.json();
    
    setIsLoggedIn(true);
    setMemberId(loginData.memberId);
    setCartItems(mergedCart);
  };

  return (
    <div>
      <h2>장바구니</h2>
      {cartItems.map(item => (
        <div key={item.id}>
          <p>상품 옵션 ID: {item.productOptionId}</p>
          <p>수량: {item.count}</p>
          <p>상태: {item.isGuest ? '비회원' : '회원'}</p>
        </div>
      ))}
    </div>
  );
}
```

## 주의사항

### 1. 세션 쿠키 설정
모든 API 요청 시 `credentials: 'include'` 옵션을 반드시 포함해야 합니다.

### 2. CORS 설정
백엔드에서 CORS 설정이 필요합니다:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true); // 중요!
    }
}
```

### 3. 세션 타임아웃
application.yml에서 세션 타임아웃을 설정할 수 있습니다:
```yaml
server:
  servlet:
    session:
      timeout: 30m  # 30분
```

### 4. 동일 상품 처리
- 비회원/회원 모두 동일한 상품을 추가하면 수량이 자동으로 합산됩니다
- 병합 시에도 동일한 로직이 적용됩니다

## 데이터베이스 마이그레이션

마이그레이션 스크립트는 `src/main/resources/db/migration/V1__add_session_id_to_cart.sql`에 위치합니다.

주요 변경사항:
- `member_id` 컬럼을 NULL 허용으로 변경
- `session_id` 컬럼 추가 (VARCHAR(255))
- 성능 최적화를 위한 인덱스 추가
  - `idx_cart_session_id`
  - `idx_cart_session_product`
  - `idx_cart_member_product`

## 테스트 시나리오

### 시나리오 1: 비회원 장바구니 사용
1. 비회원 상태에서 상품 A를 장바구니에 추가
2. 상품 B를 장바구니에 추가
3. 비회원 장바구니 조회 → A, B 확인
4. 브라우저 새로고침
5. 비회원 장바구니 조회 → A, B 여전히 유지 확인

### 시나리오 2: 로그인 후 병합
1. 비회원 상태에서 상품 A, B를 장바구니에 추가
2. 로그인 (memberId = 100)
3. 병합 API 호출 `/carts/merge/100`
4. 회원 장바구니 조회 → A, B가 회원 장바구니로 이동 확인
5. 비회원 장바구니 조회 → 비어있음 확인

### 시나리오 3: 중복 상품 병합
1. 회원 상태에서 상품 A(수량 2) 장바구니에 추가
2. 로그아웃
3. 비회원 상태에서 상품 A(수량 3) 장바구니에 추가
4. 로그인 후 병합
5. 회원 장바구니 조회 → 상품 A(수량 5) 확인

## 보안 고려사항

1. **세션 고정 공격 방지**: 로그인 시 새로운 세션 ID 발급
2. **세션 하이재킹 방지**: HTTPS 사용, Secure 쿠키 설정
3. **개인정보 보호**: 세션 데이터는 서버에만 저장
