package com.usinsa.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 응답 DTO
 * - 성공/실패 여부, HTTP 상태 코드, 에러 정보, 데이터를 포함
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RsData<T> {
    private boolean success;     // 요청 성공 여부 (비즈니스 레벨)
    private int status;          // HTTP 상태 코드 (ex. 200, 400, 401)
    private ErrorDetail error;   // 실패 시 세부 코드/메시지
    private T data;              // 성공 시 반환 데이터

    /**
     * 성공 응답 (데이터 포함)
     */
    public static <T> RsData<T> of(String code, String message, T data) {
        return RsData.<T>builder()
                .success(true)
                .status(200)
                .error(null)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 (데이터 없음)
     */
    public static <T> RsData<T> of(String code, String message) {
        return RsData.<T>builder()
                .success(true)
                .status(200)
                .error(null)
                .data(null)
                .build();
    }

    /**
     * 실패 응답
     */
    public static <T> RsData<T> error(int status, String code, String message) {
        return RsData.<T>builder()
                .success(false)
                .status(status)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .build())
                .data(null)
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetail {
        private String code;     // 비즈니스 에러 코드 (ex. MEMBER_NOT_FOUND)
        private String message;  // 사용자 친화 메시지
    }
}
