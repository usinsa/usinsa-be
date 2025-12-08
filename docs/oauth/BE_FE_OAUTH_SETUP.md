# BE-FE OAuth 2.0 연동 설정 가이드

## 📌 빠른 시작

### 현재 상태
- ✅ BE: OAuth 설정 완료, CORS 설정 완료
- ⏳ FE: 구현 필요 (아래 가이드 참고)

### BE 포트
- http://localhost:8080

### FE 포트
- http://localhost:5173 (Vite)

### OAuth 제공자
- ✅ Google
- ✅ Naver  
- ✅ Kakao

---

## 🔧 BE 설정 (완료)

### 1. CORS 설정
**파일**: `src/main/java/com/usinsa/backend/global/config/CorsConfig.java`

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173"  // ← FE 도메인
        ));
        // ...
    }
}
```

### 2. OAuth Success Handler
**파일**: `src/main/java/.../auth/oauth/handler/OAuth2AuthenticationSuccessHandler.java`

```java
private String getRedirectUrl() {
    return "http://localhost:5173/oauth/redirect";
}
```

**동작:**
- OAuth 로그인 성공 시 FE로 리다이렉트
- JWT 토큰을 Query Parameter로 전달
  ```
  http://localhost:5173/oauth/redirect
    ?accessToken=eyJhbG...
    &refreshToken=eyJhbG...
  ```

---

## 🎨 FE 구현 가이드

### 1. 로그인 버튼
```jsx
// 어디서든 사용 가능
const handleGoogleLogin = () => {
  window.location.href = 'http://localhost:8080/oauth2/authorization/google';
};

<button onClick={handleGoogleLogin}>Google로 로그인</button>
```

### 2. OAuth Callback 페이지
**경로**: `/oauth/redirect`

```jsx
// src/pages/auth/OAuthRedirect.jsx
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const OAuthRedirect = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');

    if (accessToken && refreshToken) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      navigate('/', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [searchParams, navigate]);

  return <div>로그인 처리 중...</div>;
};
```

### 3. Axios 인터셉터
```javascript
// src/api/axios.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080'
});

// 모든 요청에 JWT 추가
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 에러 시 토큰 갱신
api.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post(
        'http://localhost:8080/api/v1/auth/refresh',
        { refreshToken }
      );
      
      const { accessToken, refreshToken: newRefreshToken } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      
      error.config.headers.Authorization = `Bearer ${accessToken}`;
      return api(error.config);
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 🧪 테스트 방법

### 1. BE 실행
```bash
cd C:\Users\rhwog\IdeaProjects\usinsa-be
# IntelliJ에서 UsinsaApplication 실행
```

### 2. FE 실행
```bash
cd C:\Users\rhwog\IdeaProjects\usinsa-fe
npm run dev
```

### 3. 테스트
1. http://localhost:5173 접속
2. "Google로 로그인" 버튼 클릭
3. Google 로그인 완료
4. FE로 돌아와서 localStorage 확인
   - F12 → Application → Local Storage
   - accessToken, refreshToken 확인

### 4. API 테스트
```javascript
// F12 → Console에서 실행
fetch('http://localhost:8080/api/v1/members/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
})
.then(res => res.json())
.then(data => console.log(data));
```

---

## ⚠️ 문제 해결

### CORS 에러
```
Access blocked by CORS policy
```
**해결**: BE 재시작 (CorsConfig 적용 확인)

### 404 에러 (/oauth/redirect)
```
Cannot GET /oauth/redirect
```
**해결**: FE 라우터에 `/oauth/redirect` 경로 추가

### 토큰이 없음
```
accessToken: null
```
**해결**: BE OAuth2AuthenticationSuccessHandler 로그 확인

---

## 📋 체크리스트

### BE
- [x] CorsConfig.java 생성
- [x] SecurityConfig CORS 적용
- [x] OAuth2AuthenticationSuccessHandler FE URL 설정

### FE
- [ ] 로그인 버튼 구현
- [ ] `/oauth/redirect` 라우트 추가
- [ ] OAuthRedirect 페이지 구현
- [ ] Axios 인터셉터 설정

---

**참고**: 상세 가이드는 Artifact 문서 참고
