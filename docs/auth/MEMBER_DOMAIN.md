# Member 도메인 상세 명세서

## 📋 목차
1. [개요](#개요)
2. [Member Entity 구조](#member-entity-구조)
3. [필드별 상세 설명](#필드별-상세-설명)
4. [연관 관계](#연관-관계)
5. [비즈니스 로직](#비즈니스-로직)

---

## 개요

### 목적
Usinsa 플랫폼의 회원 정보를 관리하는 핵심 도메인입니다.

### 주요 책임
- 회원 기본 정보 관리 (이메일, 비밀번호, 프로필 등)
- 회원-주문 관계 관리
- 회원-배송지 관계 관리
- 회원-장바구니 관계 관리

---

## Member Entity 구조

```java
@Entity
@Table(name="member")
public class Member {
    
    // 기본 정보
    private Long id;              // PK, Auto Increment
    private String usinaId;       // 유시나 ID (로그인 ID)
    private String password;      // 암호화된 비밀번호
    private String name;          // 실명
    private String nickname;      // 닉네임
    private String email;         // 이메일 (로그인에 사용)
    private String phone;         // 전화번호
    private String profileImage;  // 프로필 이미지 URL
    private Boolean isAdmin;      // 관리자 여부
    
    // 연관 관계
    private List<Order> orders;                      // 주문 목록
    private List<DeliveryAddress> deliveryAddresses; // 배송지 목록
    private List<Cart> carts;                        // 장바구니 목록
}
```

---

## 필드별 상세 설명

### 1. id (Long)
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**역할:**
- 회원의 고유 식별자 (Primary Key)
- DB에서 자동 생성되는 순차 번호

**왜 Long 타입인가?**
- `int` 대신 `Long` 사용 이유:
  - `@NotNull` 제약 조건을 명확히 표현 가능
  - Wrapper 클래스로 null 체크 가능
  - JPA/Hibernate와의 호환성
  - 대용량 데이터 처리 (int: 21억, long: 900경)

**사용 예시:**
```java
// JWT 토큰의 uid 클레임으로 사용
{
  "uid": 1,
  "email": "user@example.com"
}

// Spring Security Principal로 사용
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
Long memberId = (Long) auth.getPrincipal();
```

### 2. usinaId (String)
```java
@Column(name = "usina_id", nullable = false)
@NotBlank
private String usinaId;
```

**역할:**
- 유시나 플랫폼의 고유 사용자 ID
- 사용자가 직접 설정하는 로그인 ID

**왜 email과 별도로 존재하는가?**
- **usinaId**: 사용자 친화적 ID (예: `hong123`)
- **email**: 실제 로그인에 사용 (예: `hong@example.com`)

**사용 시나리오:**
```
회원가입:
  ├─ usinaId 입력 (중복 체크)
  ├─ email 입력 (중복 체크)
  └─ password 입력

로그인:
  ├─ email로 로그인 (현재 구현)
  └─ usinaId로 로그인 (향후 지원 가능)
```

### 3. password (String)
```java
@Column(name = "password", nullable = false)
@NotBlank
private String password;
```

**역할:**
- BCrypt로 암호화된 비밀번호 저장

**암호화 방식:**
```java
// 회원가입 시
String rawPassword = "mypassword123";
String encodedPassword = passwordEncoder.encode(rawPassword);
// 저장: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

// 로그인 시
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

**BCrypt 특징:**
- Salt 자동 생성 (동일 비밀번호도 다른 해시값)
- 느린 해싱 속도 (무차별 대입 공격 방어)
- 단방향 암호화 (복호화 불가)

### 4. name (String)
```java
@Column(name = "name", nullable = false)
@NotBlank
private String name;
```

**역할:**
- 회원의 실명

**사용 예시:**
- 주문 시 배송자 이름
- 회원 정보 표시
- 관리자 화면에서 회원 식별

### 5. nickname (String)
```java
@Column(name = "nickname", nullable = false)
@NotBlank
private String nickname;
```

**역할:**
- 커뮤니티/리뷰에서 사용할 닉네임

**name vs nickname:**
```
name: 홍길동 (실명, 배송/결제에 사용)
nickname: 패션왕123 (표시용, 리뷰/커뮤니티에 사용)
```

### 6. email (String)
```java
@Column(name = "email", nullable = false)
@NotBlank
private String email;
```

**역할:**
- 로그인 계정 (현재 구현)
- 알림 발송 (주문 확인, 배송 알림 등)

**유효성 검증:**
```java
// DTO에서 검증
@Email(message = "이메일 형식이 올바르지 않습니다")
private String email;
```

### 7. phone (String)
```java
@Column(name = "phone", nullable = false)
@NotBlank
private String phone;
```

**역할:**
- 회원 연락처
- 배송 시 연락용
- SMS 알림 (주문, 배송)

**형식:**
```
저장: "01012345678" (하이픈 제거)
표시: "010-1234-5678" (하이픈 추가)
```

### 8. profileImage (String)
```java
@Column(name = "profile_image")
private String profileImage;
```

**역할:**
- 프로필 이미지 URL 저장

**nullable 이유:**
- 프로필 이미지는 선택 사항
- 기본 이미지 사용 가능

**저장 형식:**
```
S3 URL: "https://usinsa-bucket.s3.ap-northeast-2.amazonaws.com/profiles/user123.jpg"
```

### 9. isAdmin (Boolean)
```java
@Builder.Default
@Column(name = "is_admin", nullable = false)
private Boolean isAdmin = false;
```

**역할:**
- 관리자 권한 여부

**@Builder.Default 이유:**
```java
// @Builder.Default 없으면:
Member member = Member.builder()
    .email("test@test.com")
    .build();
// isAdmin = null (NPE 위험!)

// @Builder.Default 있으면:
Member member = Member.builder()
    .email("test@test.com")
    .build();
// isAdmin = false (안전!)
```

**권한 변환:**
```java
// 로그인 시 권한 매핑
List<String> roles = member.getIsAdmin() 
    ? List.of("ROLE_USER", "ROLE_ADMIN")
    : List.of("ROLE_USER");
```

---

## 연관 관계

### 1. Order (일대다)
```java
@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> Order = new ArrayList<>();
```

**연관 관계 설명:**
- **일대다**: 한 회원은 여러 주문을 가질 수 있음
- **mappedBy**: Order 엔티티의 `member` 필드가 외래키 주인
- **cascade = ALL**: 회원 삭제 시 모든 주문도 삭제
- **orphanRemoval**: 주문이 리스트에서 제거되면 DB에서도 삭제

**왜 CascadeType.ALL?**
```
회원 탈퇴 시나리오:
├─ 회원 삭제 요청
├─ 연관된 모든 주문 자동 삭제
├─ 주문 상품도 자동 삭제
└─ 데이터 일관성 유지
```

**주의사항:**
```java
// ⚠️ 실무에서는 주문 삭제보다는 상태 변경
member.setStatus(MemberStatus.WITHDRAWN);
// 주문은 히스토리로 보존
```

### 2. DeliveryAddress (일대다)
```java
@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
private List<DeliveryAddress> deliveryAddresses = new ArrayList<>();
```

**연관 관계 설명:**
- 한 회원은 여러 배송지를 등록 가능
- 회원 삭제 시 모든 배송지 삭제

**사용 시나리오:**
```
회원 홍길동의 배송지:
├─ 집 (기본 배송지)
├─ 회사
└─ 부모님 댁
```

### 3. Cart (일대다)
```java
@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Cart> carts = new ArrayList<>();
```

**연관 관계 설명:**
- 한 회원은 여러 장바구니 항목을 가질 수 있음
- 회원 삭제 시 장바구니도 삭제

**장바구니 구조:**
```
회원 홍길동의 장바구니:
├─ 상품 A (옵션: 블랙/L) - 수량: 2
├─ 상품 B (옵션: 화이트/M) - 수량: 1
└─ 상품 C (옵션: 네이비/XL) - 수량: 3
```

---

## 비즈니스 로직

### update() 메서드
```java
public void update(String name, String nickname, String email, 
                   String phone, String profileImage, Boolean isAdmin) {
    if (name != null) this.name = name;
    if (nickname != null) this.nickname = nickname;
    if (email != null) this.email = email;
    if (phone != null) this.phone = phone;
    if (profileImage != null) this.profileImage = profileImage;
}
```

**Null 체크 이유:**
- 부분 업데이트 지원
- 변경하지 않는 필드는 null로 전달

**사용 예시:**
```java
// 닉네임만 변경
member.update(null, "새닉네임", null, null, null, null);

// 여러 필드 동시 변경
member.update("홍길동", "패션왕", "hong@new.com", null, null, null);
```

**개선 방향:**
```java
// Builder 패턴 활용
public void update(MemberUpdateDto dto) {
    Optional.ofNullable(dto.getName()).ifPresent(v -> this.name = v);
    Optional.ofNullable(dto.getNickname()).ifPresent(v -> this.nickname = v);
    // ...
}
```

---

## 데이터베이스 설계

### 테이블 스키마
```sql
CREATE TABLE member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usina_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    profile_image VARCHAR(500),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_member_email ON member(email);
CREATE INDEX idx_member_usina_id ON member(usina_id);
```

### 제약 조건
- **email UNIQUE**: 이메일 중복 방지 (로그인 계정)
- **usinaId UNIQUE**: 유시나 ID 중복 방지
- **NOT NULL**: 필수 입력 필드 강제

---

## 보안 고려사항

### 1. 비밀번호 암호화
```java
// 절대 평문 저장 금지!
member.setPassword("mypassword"); // ❌

// BCrypt로 암호화
String encoded = passwordEncoder.encode("mypassword");
member.setPassword(encoded); // ✅
```

### 2. 개인정보 보호
```java
// 응답 DTO에서 민감정보 제외
public class MemberResponseDto {
    private Long id;
    private String name;
    private String nickname;
    // password, phone은 제외!
}
```

### 3. 권한 검증
```java
// 자신의 정보만 수정 가능
if (!memberId.equals(currentUserId)) {
    throw new CustomException(ErrorCode.FORBIDDEN);
}
```

---

## 향후 개선 사항

### 1. 감사 (Auditing) 추가
```java
@EntityListeners(AuditingEntityListener.class)
public class Member {
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 2. Soft Delete
```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

@Column(name = "status")
@Enumerated(EnumType.STRING)
private MemberStatus status; // ACTIVE, WITHDRAWN, SUSPENDED
```

### 3. 역할 기반 권한 (RBAC)
```java
@ManyToMany
@JoinTable(
    name = "member_role",
    joinColumns = @JoinColumn(name = "member_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles;
```

---

## 참고 자료
- [JPA Best Practices](https://vladmihalcea.com/tutorials/hibernate/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
