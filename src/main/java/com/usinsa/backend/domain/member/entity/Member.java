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
