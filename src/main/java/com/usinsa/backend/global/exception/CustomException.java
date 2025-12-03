package com.usinsa.backend.global.exception;

import lombok.Getter;

/**
 * 커스텀 예외 클래스
 * ErrorCode를 기반으로 예외를 생성
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
