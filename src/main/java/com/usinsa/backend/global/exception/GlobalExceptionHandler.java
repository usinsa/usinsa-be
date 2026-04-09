package com.usinsa.backend.global.exception;

import com.usinsa.backend.global.dto.RsData;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 핸들러
 * 모든 예외를 중앙에서 처리하여 일관된 응답 형식 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<RsData<Void>> handleCustomException(CustomException e) {
        log.error("CustomException: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(RsData.error(errorCode.getStatus().value(), errorCode.getCode(), e.getMessage()));
    }

    /**
     * Spring Security 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RsData<Void>> handleAuthenticationException(AuthenticationException e) {
        log.error("AuthenticationException: {}", e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(RsData.error(
                        ErrorCode.UNAUTHORIZED.getStatus().value(),
                        ErrorCode.UNAUTHORIZED.getCode(),
                        ErrorCode.UNAUTHORIZED.getMessage()
                ));
    }

    /**
     * Spring Security 인가 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RsData<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException: {}", e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getStatus())
                .body(RsData.error(
                        ErrorCode.FORBIDDEN.getStatus().value(),
                        ErrorCode.FORBIDDEN.getCode(),
                        ErrorCode.FORBIDDEN.getMessage()
                ));
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("ValidationException: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(RsData.error(
                        ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage
                ));
    }

    /**
     * EntityNotFoundException 처리
     * JPA에서 엔티티를 찾을 수 없을 때 발생
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RsData<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("EntityNotFoundException: {}", e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.ENTITY_NOT_FOUND.getStatus())
                .body(RsData.error(
                        ErrorCode.ENTITY_NOT_FOUND.getStatus().value(),
                        ErrorCode.ENTITY_NOT_FOUND.getCode(),
                        e.getMessage()
                ));
    }

    /**
     * IllegalArgumentException 처리
     * 잘못된 인자가 전달되었을 때 발생
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RsData<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(RsData.error(
                        ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        e.getMessage()
                ));
    }

    /**
     * IllegalStateException 처리
     * 현재 상태에서 호출할 수 없는 메서드가 호출되었을 때 발생
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RsData<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error("IllegalStateException: {}", e.getMessage());
        
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(RsData.error(
                        ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        e.getMessage()
                ));
    }

    /**
     * MethodArgumentTypeMismatchException 처리
     * 파라미터 타입이 일치하지 않을 때 발생
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.error("MethodArgumentTypeMismatchException: {}", e.getMessage());
        
        String errorMessage = String.format(
                "파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다.",
                e.getName(),
                e.getValue()
        );
        
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(RsData.error(
                        ErrorCode.INVALID_INPUT_VALUE.getStatus().value(),
                        ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage
                ));
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException(Exception e) {
        log.error("Exception: ", e);
        
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(RsData.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }
}
