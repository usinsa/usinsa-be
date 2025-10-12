package com.usinsa.backend.domain.deliveryAddress.dto;

import com.usinsa.backend.domain.deliveryAddress.entity.DeliveryAddress;
import com.usinsa.backend.domain.member.entity.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class DeliveryAddressDto {

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CreateReq {
        @NotNull(message = "회원 ID는 필수 입력 값입니다.")
        private Long memberId;

        @NotBlank(message = "수령인 이름은 필수 입력 값입니다.")
        private String receiverName;

        @NotBlank(message = "수령인 전화번호는 필수 입력 값입니다.")
        private String receiverPhone;

        @NotBlank(message = "수령인 주소는 필수 입력 값입니다.")
        private String receiverAddress;

        public DeliveryAddress toEntity(Member member) {
            return DeliveryAddress.builder()
                    .member(member)
                    .receiverName(receiverName)
                    .receiverPhone(receiverPhone)
                    .receiverAddress(receiverAddress)
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        @NotBlank(message = "수령인 이름은 필수 입력 값입니다.")
        private String receiverName;

        @NotBlank(message = "수령인 전화번호는 필수 입력 값입니다.")
        private String receiverPhone;

        @NotBlank(message = "수령인 주소는 필수 입력 값입니다.")
        private String receiverAddress;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long deliveryAddressId;
        private Long memberId;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
    }
}