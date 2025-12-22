package com.usinsa.backend.global.session;

import com.usinsa.backend.domain.cart.service.CartService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP 세션 생명주기를 감지하여 비회원 장바구니를 정리하는 리스너
 * 
 * 주요 기능:
 * - 세션 생성 시 로깅
 * - 세션 종료/만료 시 해당 세션의 비회원 장바구니 자동 삭제
 * 
 * 동작 시나리오:
 * 1. 비회원이 장바구니에 상품 추가 → 세션 ID로 장바구니 생성
 * 2. 비회원이 로그인하여 병합 → 세션 장바구니는 회원 장바구니로 전환
 * 3. 비회원이 로그인하지 않고 세션 종료 → 이 리스너가 세션 장바구니 자동 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupListener implements HttpSessionListener {

    private final CartService cartService;

    /**
     * 세션 생성 시 호출
     * 디버깅 및 모니터링 목적으로 로그만 기록
     */
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        log.debug("세션 생성됨 - SessionId: {}", sessionId);
    }

    /**
     * 세션 종료 시 호출 (명시적 invalidate() 또는 타임아웃)
     * 해당 세션의 비회원 장바구니를 DB에서 삭제
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        
        try {
            log.info("세션 종료 감지 - SessionId: {}, 비회원 장바구니 정리 시작", sessionId);
            
            // 해당 세션의 비회원 장바구니 삭제
            int deletedCount = cartService.deleteGuestCartBySessionId(sessionId);
            
            if (deletedCount > 0) {
                log.info("비회원 장바구니 정리 완료 - SessionId: {}, 삭제된 항목 수: {}", 
                        sessionId, deletedCount);
            } else {
                log.debug("정리할 비회원 장바구니 없음 - SessionId: {}", sessionId);
            }
            
        } catch (Exception e) {
            // 세션 종료 처리 중 예외가 발생해도 세션 종료 자체는 진행되어야 함
            log.error("비회원 장바구니 정리 중 오류 발생 - SessionId: {}, Error: {}", 
                    sessionId, e.getMessage(), e);
        }
    }
}
