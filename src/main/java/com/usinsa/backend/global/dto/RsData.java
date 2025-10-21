package com.usinsa.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RsData<T> {
    private boolean success;     // 요청 성공 여부 (비즈니스 레벨)
    private int status;          // HTTP 상태 코드 (ex. 200, 400, 401)
    private ErrorDetail error;   // 실패 시 세부 코드/메시지
    private T data;              // 성공 시 반환 데이터

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetail {
        private String code;     // 비즈니스 에러 코드 (ex. MEMBER_NOT_FOUND)
        private String message;  // 사용자 친화 메시지
    }
}
