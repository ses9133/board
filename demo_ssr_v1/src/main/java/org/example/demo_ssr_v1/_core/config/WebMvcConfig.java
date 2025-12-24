package org.example.demo_ssr_v1._core.config;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.interceptor.AdminInterceptor;
import org.example.demo_ssr_v1._core.interceptor.LoginInterceptor;
import org.example.demo_ssr_v1._core.interceptor.SessionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 클래스
 * */
@Configuration // @Component 클래스 내부에서 @Bean 어노테이션을 사용해야한다면 @Configuration 사용해야함
                // 내부도 IoC 대상 여부 확인
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final SessionInterceptor sessionInterceptor;
    private final AdminInterceptor adminInterceptor;

    // 인터셉터는 여러개 등록 가능
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                        .addPathPatterns("/**");

        // 1. 설정에 LoginInterceptor를 등록하는 코드
        // 2. 인터셉터가 동작할 URL 패턴 지정
        // 3. 어떤 URL 요청이 로그인 여부를 필요로 할 지 확인해야함
        // /board/** -> 일단 이 엔드포인트 다 검사시킬것임
        // /user/** -> 일단 이 엔드포인트 다 검사시킬것임
        // 단, 특정 URL 은 제외시킬 것임
        registry.addInterceptor(loginInterceptor)
                // /** -> 모든 URL 이 제외 대상이 됨
                .addPathPatterns("/board/**", "/user/**", "/reply/**", "/admin/**")
                .excludePathPatterns(
                        "/login",
                        "/join",
                        "/logout",
                        "/user/kakao",
                        "/board/list",
                        "/",
                        "/board/{id:\\d+}",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.io",
                        "/h2-console/**"
                );

        registry.addInterceptor(adminInterceptor)
                // 관리자 전용 페이지에만 적용처리
                .addPathPatterns("/admin/**");
    }

    /**
     * 정적 리소스 핸들러
     * 업로드된 이미지 파일을 웹에서 접근할 수 있도록 설정
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         *  /images/** 경로로 요청이 들어오면 나의 폴더 images/ 디렉토리에서 찾게 설정합니다.
         */
        // 머스태치 이미지 태그에 src 경로에 /images/** 같은 경로로 설정되어있다면 스프링이 알아서
        // 내 폴더 file: (프로젝트 루트 디렉토리 의미)안에 images/ 폴더를 찾게 함
//        registry.addResourceHandler("/images/**")
//                .addResourceLocations("file:images/");

        // file: 파일 시스템을 가르킨다.
        // 파일 시스템에서 절대 경로를 의미하는 URI 표기법은 /// (/ 3개)
        // file:images/ 는 앞에 슬러시가 없기 때문에 상대 경로를 의미한다.
        // file:///D:/uploads/ --> 내 컴퓨터 절대 경로를 의미한다.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///D:/uploads/");
    }

    @Bean // IoC 처리 (스프링 컨테이너에 싱글톤 패턴으로 객체가 메모리에 올라간다)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
