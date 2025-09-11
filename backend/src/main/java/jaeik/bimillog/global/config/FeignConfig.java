package jaeik.bimillog.global.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>OpenFeign 설정 클래스</h2>
 * <p>OpenFeign 클라이언트 설정을 담당하는 클래스입니다.</p>
 * <p>타임아웃 설정, 에러 디코더 설정, 로깅 설정</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Configuration
@EnableFeignClients(basePackages = "jaeik.bimillog.infrastructure.adapter.common.client")
public class FeignConfig {

    /**
     * <h3>Feign 로그 레벨 설정</h3>
     * <p>개발 환경에서 HTTP 요청/응답을 로깅하기 위한 설정입니다.</p>
     *
     * @return Feign 로거 레벨 (BASIC)
     * @author Jaeik
     * @since 2.0.0
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * <h3>Feign 에러 디코더 설정</h3>
     * <p>HTTP 4xx, 5xx 응답에 대한 커스텀 에러 처리를 정의합니다.</p>
     *
     * @return 커스텀 에러 디코더
     * @author Jaeik
     * @since 2.0.0
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            switch (response.status()) {
                case 400:
                    return new RuntimeException("Bad Request: " + methodKey);
                case 401:
                    return new RuntimeException("Unauthorized: " + methodKey);
                case 403:
                    return new RuntimeException("Forbidden: " + methodKey);
                case 404:
                    return new RuntimeException("Not Found: " + methodKey);
                case 500:
                    return new RuntimeException("Internal Server Error: " + methodKey);
                default:
                    return new RuntimeException("HTTP Error " + response.status() + ": " + methodKey);
            }
        };
    }
}