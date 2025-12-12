package org.example.demo_ssr_v1._core.errors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// @ControllerAdvice - 모든 컨트롤러에서 발생하는 예외를 이 클래스에서 중앙 집중화 시킴
@ControllerAdvice
@Slf4j
public class MyExceptionHandler {

    // 내가 지켜볼 예외를 명시해주면 ControllerAdvice가 가지고 와서 처리함
    @ExceptionHandler(Exception400.class)  // Exception400 예외를 잡음
    public String ex400(Exception400 e, HttpServletRequest request) {
        log.warn("== 400 에러 발생 ==");
        log.warn("요청 URL: {}", request.getRequestURL()); // 사용자가 던진 URL 확인 가능
        log.warn("에러 메시지: {}", e.getMessage());
        log.warn("예외 클래스: {}", e.getClass().getSimpleName());

        return "err/400";
    }
}