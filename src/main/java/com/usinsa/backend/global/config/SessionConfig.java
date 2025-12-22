package com.usinsa.backend.global.config;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.usinsa.backend.global.session.SessionCleanupListener;

/**
 * HTTP 세션 관련 설정
 * 
 * 주요 설정:
 * - 세션 리스너 등록: 세션 생명주기 감지 및 비회원 장바구니 정리
 * 
 * 세션 타임아웃 설정은 application.yml에서 관리:
 * spring.session.timeout: 30m
 * 
 * 세션 쿠키 설정 (HttpOnly, SameSite 등)은 
 * server.servlet.session.cookie.* 속성으로 설정 가능
 */
@Configuration
public class SessionConfig {

    /**
     * 세션 정리 리스너를 서블릿 컨테이너에 등록
     * 
     * SessionCleanupListener는 이미 @Component로 스프링 빈으로 등록
     * 이를 ServletListenerRegistrationBean으로 감싸서 서블릿 컨테이너에 등록
     * 
     * 동작:
     * - 세션 생성 시: sessionCreated() 호출 (로깅)
     * - 세션 종료 시: sessionDestroyed() 호출 (비회원 장바구니 삭제)
     */
    @Bean
    public ServletListenerRegistrationBean<SessionCleanupListener> sessionListener(
            SessionCleanupListener sessionCleanupListener) {
        ServletListenerRegistrationBean<SessionCleanupListener> listenerBean =
                new ServletListenerRegistrationBean<>();
        listenerBean.setListener(sessionCleanupListener);
        return listenerBean;
    }
}
