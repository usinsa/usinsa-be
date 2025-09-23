package com.usinsa.backend.domain.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name="member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(name = "usina_id", nullable = false)
    @NotBlank
    private String usinaId;

    @Column(name = "password", nullable = false)
    @NotBlank
    private String password;

    @Column(name = "name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "nickname", nullable = false)
    @NotBlank
    private String nickname;

    @Column(name = "email", nullable = false)
    @NotBlank
    private String email;

    @Column(name = "phone", nullable = false)
    @NotBlank
    private String phone;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;
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