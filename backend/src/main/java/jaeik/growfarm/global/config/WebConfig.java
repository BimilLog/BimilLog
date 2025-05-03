package jaeik.growfarm.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 * 웹 설정 클래스
 * WebMvcConfigurer 인터페이스를 구현하여 Spring MVC의 설정을 커스터마이징
 * addInterceptors 메서드를 오버라이드하여 인터셉터를 등록
 * LoggingInterceptor를 등록하여 모든 요청에 대해 로깅을 수행
 * 에러 페이지는 제외
 * 수정일 : 2025-05-03
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error");
    }
}