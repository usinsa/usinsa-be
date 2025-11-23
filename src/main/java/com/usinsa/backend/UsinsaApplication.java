package com.usinsa.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 비동기 이벤트 처리 활성, ES 이벤트 처리
public class UsinsaApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsinsaApplication.class, args);
    }

}
