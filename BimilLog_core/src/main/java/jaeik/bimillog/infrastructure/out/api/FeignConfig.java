package jaeik.bimillog.infrastructure.out.api;

import feign.Logger;
import feign.codec.ErrorDecoder;
import feign.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <h2>OpenFeign 설정 클래스</h2>
 * <p>OpenFeign 클라이언트 설정을 담당하는 클래스입니다.</p>
 * <p>타임아웃 설정, 에러 디코더 설정, 로깅 설정</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Configuration
@EnableFeignClients(basePackages = "jaeik/bimillog/infrastructure/out/api")
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
            String responseBody = extractBody(response);
            log.warn("Feign 호출 실패 - methodKey: {}, status: {}, reason: {}, url: {}, body: {}",
                    methodKey,
                    response.status(),
                    response.reason(),
                    response.request() != null ? response.request().url() : "unknown",
                    responseBody);

            return switch (response.status()) {
                case 400 -> new RuntimeException("Bad Request: " + methodKey);
                case 401 -> new RuntimeException("Unauthorized: " + methodKey);
                case 403 -> new RuntimeException("Forbidden: " + methodKey);
                case 404 -> new RuntimeException("Not Found: " + methodKey);
                case 500 -> new RuntimeException("Internal Server Error: " + methodKey);
                default -> new RuntimeException("HTTP Error " + response.status() + ": " + methodKey);
            };
        };
    }

    private String extractBody(feign.Response response) {
        if (response.body() == null) {
            return "no response body";
        }

        try (var reader = response.body().asReader(StandardCharsets.UTF_8)) {
            return Util.toString(reader);
        } catch (IOException ioException) {
            log.warn("Feign 응답 본문 읽기 실패 - url: {}, message: {}",
                    response.request() != null ? response.request().url() : "unknown",
                    ioException.getMessage());
            return "failed to read response body";
        }
    }
}
