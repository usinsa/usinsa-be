package com.usinsa.backend.domain.member.dto;

import com.usinsa.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReq{
        private String usinaId;
        private String password;
        private String name;
        private String nickname;
        private String email;
        private String phone;
        private String profileImage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq{
        private String password;
        private String name;
        private String nickname;
        private String email;
        private String phone;
        private String profileImage;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Resopnse {
        private Long Id;
        private String usinaId;
        private String name;
        private String nickname;
        private String email;
        private String phone;
        private String profileImage;
        private Boolean isAdmin;

        public static Resopnse fromEntity(Member member) {
            return Resopnse.builder()
                    .Id(member.getId())
                    .usinaId(member.getUsinaId())
                    .name(member.getName())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .phone(member.getPhone())
                    .profileImage(member.getProfileImage())
                    .isAdmin(member.getIsAdmin())
                    .build();
        }
    }
}
