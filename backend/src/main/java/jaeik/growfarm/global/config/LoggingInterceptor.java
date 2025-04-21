package jaeik.growfarm.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String SEPARATOR = "\n" + "=".repeat(80) + "\n";

    // 로깅할 헤더 이름 목록
    private static final Set<String> HEADERS_TO_LOG = new HashSet<>(Arrays.asList(
            "host", "origin", "referer", "cookie", "access-control-allow-origin",
            "access-control-allow-credentials", "access-control-expose-headers"));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (log.isDebugEnabled()) {
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
            StringBuilder sb = new StringBuilder(SEPARATOR);
            sb.append("Frontend Request: \n");
            sb.append(request.getMethod()).append(" ").append(request.getRequestURI()).append("\n");

            // 필터링된 헤더만 로깅
            request.getHeaderNames().asIterator()
                    .forEachRemaining(name -> {
                        if (HEADERS_TO_LOG.contains(name.toLowerCase())) {
                            sb.append(name).append(": ").append(request.getHeader(name)).append("\n");
                        }
                    });

            // 요청 body 로깅 (필요한 경우)
            String body = new String(requestWrapper.getContentAsByteArray());
            if (!body.isEmpty()) {
                sb.append("Body: ").append(body).append("\n");
            }
            sb.append(SEPARATOR);
            log.debug(sb.toString());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) throws IOException {
        if (log.isDebugEnabled()) {
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            StringBuilder sb = new StringBuilder(SEPARATOR);
            sb.append("Frontend Response: \n");
            sb.append(response.getStatus()).append("\n");

            // 필터링된 헤더만 로깅
            response.getHeaderNames()
                    .forEach(name -> {
                        if (HEADERS_TO_LOG.contains(name.toLowerCase())) {
                            sb.append(name).append(": ").append(response.getHeader(name)).append("\n");
                        }
                    });

            // 응답 body 로깅 (필요한 경우)
            String body = new String(responseWrapper.getContentAsByteArray());
            if (!body.isEmpty() && body.length() < 100) { // 길이 제한 추가
                sb.append("Body: ").append(body).append("\n");
            } else if (!body.isEmpty()) {
                sb.append("Body: [content too long to display]\n");
            }
            sb.append(SEPARATOR);
            log.debug(sb.toString());

            // 응답 body를 다시 써주기 (한 번만 읽을 수 있으므로)
            responseWrapper.copyBodyToResponse();
        }
    }
}