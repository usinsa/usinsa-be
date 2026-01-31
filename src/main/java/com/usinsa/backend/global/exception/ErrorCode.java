package com.usinsa.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // JWT 관련 에러
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "JWT_001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT_002", "만료된 토큰입니다."),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "JWT_003", "폐기된 토큰입니다."),
    TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "JWT_004", "재사용된 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "JWT_005", "블랙리스트에 등록된 토큰입니다."),
    TOKEN_TYPE_MISMATCH(HttpStatus.UNAUTHORIZED, "JWT_006", "토큰 타입이 일치하지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWT_007", "토큰이 존재하지 않습니다."),

    // 인증/인가 관련 에러
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 일치하지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_004", "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_005", "이미 사용 중인 이메일입니다."),
    
    // 회원 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원을 찾을 수 없습니다."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER_002", "이미 존재하는 회원입니다."),
    
    // 상품 관련 에러
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_002", "상품 재고가 부족합니다."),
    PRODUCT_ALREADY_LIKED(HttpStatus.CONFLICT, "PRODUCT_003", "이미 좋아요를 누른 상품입니다."),
    PRODUCT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_004", "좋아요를 누르지 않은 상품입니다."),
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_005", "상품 옵션을 찾을 수 없습니다."),
    
    // 장바구니 관련 에러
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001", "장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_002", "장바구니 상품을 찾을 수 없습니다."),
    SESSION_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CART_003", "세션 ID가 필요합니다."),
    
    // 주문 관련 에러
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "ORDER_002", "이미 취소된 주문입니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "ORDER_003", "취소할 수 없는 주문입니다."),
    ORDERED_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_004", "주문 상품을 찾을 수 없습니다."),
    
    // 배송 관련 에러
    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_001", "배송 정보를 찾을 수 없습니다."),
    DELIVERY_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "DELIVERY_002", "배송지를 찾을 수 없습니다."),
    
    // 결제 관련 에러
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_TID_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_002", "결제 TID를 찾을 수 없습니다."),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PAYMENT_003", "이미 완료된 결제입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_004", "결제 처리에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_005", "결제 취소에 실패했습니다."),
    
    // 카테고리 관련 에러
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_001", "카테고리를 찾을 수 없습니다."),
    
    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_003", "허용되지 않은 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 데이터를 찾을 수 없습니다."),
    INVALID_PROVIDER_TYPE(HttpStatus.BAD_REQUEST, "COMMON_005", "유효하지 않은 제공자 타입입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
