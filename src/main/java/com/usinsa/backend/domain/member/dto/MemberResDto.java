package com.usinsa.backend.domain.member.dto;

import com.usinsa.backend.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberResDto {
    private Long memberId;
    private String usinaId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private String profileImage;
    private Boolean isAdmin;

    public static MemberResDto from(Member member) {
        return new MemberResDto(
                member.getMemberId(),
                member.getUsinaId(),
                member.getName(),
                member.getNickname(),
                member.getEmail(),
                member.getPhone(),
                member.getProfileImage(),
                member.getIsAdmin()
        );
    }
}
