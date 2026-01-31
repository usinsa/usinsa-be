package com.usinsa.backend.domain.payment.service;

import com.usinsa.backend.domain.payment.config.KakaoPayProperties;
import com.usinsa.backend.domain.payment.dto.KakaoPayDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    private final WebClient kakaoPayWebClient;
    private final KakaoPayProperties kakaoPayProperties;

    /**
     * 카카오페이 결제 준비
     */
    public KakaoPayDto.ReadyResponse ready(KakaoPayDto.ReadyRequest request) {
        log.info("카카오페이 결제 준비 요청: {}", request.getPartnerOrderId());

        return kakaoPayWebClient.post()
                .uri("/online/v1/payment/ready")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("카카오페이 결제 준비 실패: {}", body);
                            return new RuntimeException("카카오페이 결제 준비 실패: " + body);
                        }))
                .bodyToMono(KakaoPayDto.ReadyResponse.class)
                .block();
    }

    /**
     * 카카오페이 결제 승인
     */
    public KakaoPayDto.ApproveResponse approve(KakaoPayDto.ApproveRequest request) {
        log.info("카카오페이 결제 승인 요청: TID={}", request.getTid());

        return kakaoPayWebClient.post()
                .uri("/online/v1/payment/approve")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("카카오페이 결제 승인 실패: {}", body);
                            return new RuntimeException("카카오페이 결제 승인 실패: " + body);
                        }))
                .bodyToMono(KakaoPayDto.ApproveResponse.class)
                .block();
    }

    /**
     * 카카오페이 결제 취소
     */
    public KakaoPayDto.CancelResponse cancel(KakaoPayDto.CancelRequest request) {
        log.info("카카오페이 결제 취소 요청: TID={}", request.getTid());

        return kakaoPayWebClient.post()
                .uri("/online/v1/payment/cancel")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class).map(body -> {
                            log.error("카카오페이 결제 취소 실패: {}", body);
                            return new RuntimeException("카카오페이 결제 취소 실패: " + body);
                        }))
                .bodyToMono(KakaoPayDto.CancelResponse.class)
                .block();
    }

    /**
     * CID 조회
     */
    public String getCid() {
        return kakaoPayProperties.getCid();
    }
}
