package org.example.demo_ssr_v1._core.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.demo_ssr_v1._core.errors.exception.Exception401;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1.user.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 컨트롤러 진입전에 조회먼저 해야함
        // 먼저 로그인이 되어진 후 확인을 해야한다.. (로그인 인터셉터 동작하고 있음)
        HttpSession session = request.getSession();
        User sessionUser = (User) session.getAttribute("sessionUser");

        // 1. 로그인 체크는 loginInterceptor 가 이미 했으므로 생략 가능하지만 안전상의 이유로 한 번 더 체크함
        if(sessionUser == null) {
            throw new Exception401("로그인이 필요합니다.");
        }

        // 2. 관리자 역할 여부 확인
        if(!sessionUser.isAdmin()) {
            throw new Exception403("관리자 권한이 필요합니다.");
        }

        return true;
    }
}
