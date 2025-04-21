package jaeik.growfarm.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
public class WebClientConfig {

    private static final String SEPARATOR = "\n" + "=".repeat(80) + "\n";

    // 로깅할 헤더 이름 목록
    private static final Set<String> HEADERS_TO_LOG = new HashSet<>(Arrays.asList(
            "host", "origin", "referer", "cookie", "authorization", "content-type",
            "location", "access-control-allow-origin", "access-control-allow-credentials"));

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(logRequest())
                .filter(logResponse());
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(SEPARATOR);
                sb.append("Kakao Request: \n");
                sb.append(clientRequest.method()).append(" ").append(clientRequest.url()).append("\n");

                // 필터링된 헤더만 로깅
                clientRequest.headers().forEach((name, values) -> {
                    if (HEADERS_TO_LOG.contains(name.toLowerCase())) {
                        values.forEach(value -> sb.append(name).append(": ").append(value).append("\n"));
                    }
                });

                sb.append(SEPARATOR);
                log.debug(sb.toString());
            }
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(SEPARATOR);
                sb.append("Kakao Response: \n");
                sb.append(clientResponse.statusCode()).append("\n");

                // 필터링된 헤더만 로깅
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                    if (HEADERS_TO_LOG.contains(name.toLowerCase())) {
                        values.forEach(value -> sb.append(name).append(": ").append(value).append("\n"));
                    }
                });

                sb.append(SEPARATOR);
                log.debug(sb.toString());
            }
            return Mono.just(clientResponse);
        });
    }
}