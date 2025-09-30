package com.usinsa.backend.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_usina_id", columnNames = {"usina_id"}),
                @UniqueConstraint(name = "uk_member_email", columnNames = {"email"})
        },
        indexes = {
                @Index(name = "idx_member_email", columnList = "email"),
                @Index(name = "idx_member_usina_id", columnList = "usina_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"password"}) // 패스워드 노출 금지
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    @Comment("서로게이트 키")
    private Long memberId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "usina_id", nullable = false, length = 40)
    @Comment("로그인/핸들 ID - 불변값(정책에 따라 변경 금지 또는 제한)")
    private String usinaId;

    @NotBlank
    @Column(name = "password", nullable = false, length = 100)
    @Comment("BCrypt 해시 저장. 평문 금지")
    private String password;

    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Size(max = 30)
    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @NotBlank
    @Email
    @Size(max = 190) // 인덱스 고려
    @Column(name = "email", nullable = false, length = 190)
    private String email;

    @NotBlank
    @Size(max = 20)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Size(max = 512)
    @Column(name = "profile_image", length = 512)
    private String profileImage;

    @Builder.Default
    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ======= 도메인 로직 =======

    /**
     * 프로필성 변경 (권한/이메일/usinaId/패스워드는 제외)
     * 왜 분리? 민감값과 일반값 분리를 통해 보안/검증/감사를 명확히 하기 위함.
     */
    public void updateProfile(String name, String nickname, String phone, String profileImage) {
        if (name != null) this.name = name;
        if (nickname != null) this.nickname = nickname;
        if (phone != null) this.phone = normalizePhone(phone);
        if (profileImage != null) this.profileImage = profileImage;
    }

    /**
     * 관리자 권한 변경 (관리자 전용 유스케이스에서만 호출)
     */
    public void changeAdminFlag(boolean admin) {
        this.isAdmin = admin;
    }

    /**
     * 이메일은 불변 취급(로그인/식별/고지).
     * 변경 유스케이스가 있다면 별도 검증/중복체크/감사 필요.
     */
    public void changeEmail(String newEmail) {
        if (newEmail == null || newEmail.isBlank()) return;
        this.email = normalizeEmail(newEmail);
    }

    /**
     * 패스워드 변경은 반드시 해시가 넘어오도록 강제.
     * (rawPassword를 받지 않음. 해시는 MemberService에서 생성)
     */
    public void changePasswordHash(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) return;
        this.password = encodedPassword;
    }

    // 권한 계산: Security 쪽에서 사용
    public String role() {
        return Boolean.TRUE.equals(isAdmin) ? "ROLE_ADMIN" : "ROLE_USER";
    }

    // ======= 정규화 유틸 =======
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
    private static String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", ""); // 숫자만 보존
    }

    // 생성 편의 메서드: 서비스에서 신규 생성 시 사용 (해시는 서비스가 주입)
    public static Member createNew(String usinaId, String encodedPassword, String name,
                                   String nickname, String email, String phone, String profileImage) {
        Member m = Member.builder()
                .usinaId(usinaId)
                .password(encodedPassword)
                .name(name)
                .nickname(nickname)
                .email(normalizeEmail(email))
                .phone(normalizePhone(phone))
                .profileImage(profileImage)
                .isAdmin(false)
                .build();
        return m;
    }
}

/*
Entity 설계
1️⃣ @Entity를 통해 해당 클래스가 JPA Entity임을 명시.
2️⃣ @Table(name="member")를 통해 해당 Entity가 매핑될 테이블 이름을 지정.
3️⃣ @Id와 @GeneratedValue(strategy = GenerationType.IDENTITY)를 통해 memberId가 기본 키이며 자동 생성됨을 지정.
4️⃣ 각 필드에 @Column 어노테이션을 사용하여 데이터베이스 컬럼과 매핑, nullable 속성을 통해 필수 여부 지정.
5️⃣ @NotBlank 어노테이션을 통해 필수 입력 필드임을 검증.

TODO : 처음에 ERD 설계 당시에 memberId 타입을 int로 설정했었으나, NOT NULL을 사용하기 위해 wrapper class인 Long으로 변경.
*/