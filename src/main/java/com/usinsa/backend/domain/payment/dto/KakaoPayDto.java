package com.usinsa.backend.domain.payment.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class KakaoPayDto {

    /**
     * 결제 준비 요청 DTO
     */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ReadyRequest {
        private String cid;                    // 가맹점 코드
        private String partnerOrderId;         // 가맹점 주문번호
        private String partnerUserId;          // 가맹점 회원 id
        private String itemName;               // 상품명
        private Integer quantity;              // 상품 수량
        private Integer totalAmount;           // 상품 총액
        private Integer taxFreeAmount;         // 상품 비과세 금액
        private Integer vatAmount;             // 상품 부가세 금액
        private String approvalUrl;            // 결제 성공 시 redirect url
        private String cancelUrl;              // 결제 취소 시 redirect url
        private String failUrl;                // 결제 실패 시 redirect url
    }

    /**
     * 결제 준비 응답 DTO
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ReadyResponse {
        private String tid;                    // 결제 고유번호
        private String nextRedirectAppUrl;     // 요청한 클라이언트가 모바일 앱인 경우
        private String nextRedirectMobileUrl;  // 요청한 클라이언트가 모바일 웹인 경우
        private String nextRedirectPcUrl;      // 요청한 클라이언트가 PC 웹인 경우
        private String androidAppScheme;       // 카카오페이 결제화면으로 이동하는 Android 앱 스킴
        private String iosAppScheme;           // 카카오페이 결제화면으로 이동하는 iOS 앱 스킴
        private LocalDateTime createdAt;       // 결제 준비 요청 시간
    }

    /**
     * 결제 승인 요청 DTO
     */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ApproveRequest {
        private String cid;                    // 가맹점 코드
        private String tid;                    // 결제 고유번호
        private String partnerOrderId;         // 가맹점 주문번호
        private String partnerUserId;          // 가맹점 회원 id
        private String pgToken;                // 결제승인 요청을 인증하는 토큰
    }

    /**
     * 결제 승인 응답 DTO
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ApproveResponse {
        private String aid;                    // 요청 고유 번호
        private String tid;                    // 결제 고유 번호
        private String cid;                    // 가맹점 코드
        private String sid;                    // 정기결제용 ID
        private String partnerOrderId;         // 가맹점 주문번호
        private String partnerUserId;          // 가맹점 회원 id
        private String paymentMethodType;      // 결제 수단
        private Amount amount;                 // 결제 금액 정보
        private CardInfo cardInfo;             // 결제 카드 정보
        private String itemName;               // 상품명
        private String itemCode;               // 상품 코드
        private Integer quantity;              // 상품 수량
        private LocalDateTime createdAt;       // 결제 준비 요청 시각
        private LocalDateTime approvedAt;      // 결제 승인 시각
        private String payload;                // 결제 승인 요청에 대해 저장 값, 요청 시 전달 내용
    }

    /**
     * 결제 금액 정보
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Amount {
        private Integer total;                 // 전체 결제 금액
        private Integer taxFree;               // 비과세 금액
        private Integer vat;                   // 부가세 금액
        private Integer point;                 // 사용한 포인트 금액
        private Integer discount;              // 할인 금액
        private Integer greenDeposit;          // 컵 보증금
    }

    /**
     * 결제 카드 정보
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CardInfo {
        private String purchaseCorp;           // 매입 카드사 한글명
        private String purchaseCorpCode;       // 매입 카드사 코드
        private String issuerCorp;             // 카드 발급사 한글명
        private String issuerCorpCode;         // 카드 발급사 코드
        private String kakaopayPurchaseCorp;   // 카카오페이 매입사명
        private String kakaopayPurchaseCorpCode; // 카카오페이 매입사 코드
        private String kakaopayIssuerCorp;     // 카카오페이 발급사명
        private String kakaopayIssuerCorpCode; // 카카오페이 발급사 코드
        private String bin;                    // 카드 BIN
        private String cardType;               // 카드 타입
        private String installMonth;           // 할부 개월 수
        private String approvedId;             // 카드사 승인번호
        private String cardMid;                // 카드사 가맹점 번호
        private String interestFreeInstall;    // 무이자할부 여부(Y/N)
        private String installmentType;        // 할부 유형
        private String cardItemCode;           // 카드 상품 코드
    }

    /**
     * 결제 취소 요청 DTO
     */
    @Getter
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CancelRequest {
        private String cid;                    // 가맹점 코드
        private String tid;                    // 결제 고유번호
        private Integer cancelAmount;          // 취소 금액
        private Integer cancelTaxFreeAmount;   // 취소 비과세 금액
        private Integer cancelVatAmount;       // 취소 부가세 금액
    }

    /**
     * 결제 취소 응답 DTO
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CancelResponse {
        private String aid;                    // 요청 고유 번호
        private String tid;                    // 결제 고유 번호
        private String cid;                    // 가맹점 코드
        private String status;                 // 결제 상태
        private String partnerOrderId;         // 가맹점 주문번호
        private String partnerUserId;          // 가맹점 회원 id
        private String paymentMethodType;      // 결제 수단
        private Amount amount;                 // 결제 금액 정보
        private ApprovedCancelAmount approvedCancelAmount; // 이번 요청으로 취소된 금액
        private CanceledAmount canceledAmount; // 누계 취소 금액
        private CancelAvailableAmount cancelAvailableAmount; // 남은 취소 가능 금액
        private String itemName;               // 상품명
        private String itemCode;               // 상품 코드
        private Integer quantity;              // 상품 수량
        private LocalDateTime createdAt;       // 결제 준비 요청 시각
        private LocalDateTime approvedAt;      // 결제 승인 시각
        private LocalDateTime canceledAt;      // 결제 취소 시각
        private String payload;                // 취소 요청 시 전달한 값
    }

    /**
     * 이번 요청으로 취소된 금액
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ApprovedCancelAmount {
        private Integer total;                 // 이번 요청으로 취소된 전체 금액
        private Integer taxFree;               // 이번 요청으로 취소된 비과세 금액
        private Integer vat;                   // 이번 요청으로 취소된 부가세 금액
        private Integer point;                 // 이번 요청으로 취소된 포인트 금액
        private Integer discount;              // 이번 요청으로 취소된 할인 금액
        private Integer greenDeposit;          // 컵 보증금
    }

    /**
     * 누계 취소 금액
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CanceledAmount {
        private Integer total;                 // 취소된 전체 누적 금액
        private Integer taxFree;               // 취소된 비과세 누적 금액
        private Integer vat;                   // 취소된 부가세 누적 금액
        private Integer point;                 // 취소된 포인트 누적 금액
        private Integer discount;              // 취소된 할인 누적 금액
        private Integer greenDeposit;          // 컵 보증금
    }

    /**
     * 남은 취소 가능 금액
     */
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CancelAvailableAmount {
        private Integer total;                 // 취소 가능한 전체 금액
        private Integer taxFree;               // 취소 가능한 비과세 금액
        private Integer vat;                   // 취소 가능한 부가세 금액
        private Integer point;                 // 취소 가능한 포인트 금액
        private Integer discount;              // 취소 가능한 할인 금액
        private Integer greenDeposit;          // 컵 보증금
    }
}
