package jaeik.bimillog.infrastructure.filter;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * <h2>로그 필터</h2>
 * <p>HTTP 요청에 대한 로그를 기록하는 필터 클래스</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Component
public class LogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final List<String> WHITELIST = List.of(
            "/api/global/health",
            "/actuator/**"
    );

    /**
     * <h3>필터 내부 처리</h3>
     * <p>HTTP 요청의 URI와 사용자 인증 정보를 기반으로 로그를 기록합니다.</p>
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 관련 예외
     * @throws IOException 입출력 관련 예외
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean isAdminAttempt = uri.startsWith("/dto");

        long startTime = System.currentTimeMillis();
        String referer = request.getHeader("Referer");
        String ip = getClientIp(request);
        String method = request.getMethod();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserContext userContext = resolveUserContext(authentication);
        setMdcContext(userContext, ip);

        try {
            filterChain.doFilter(request, response);
        } finally {
                logRequest(isAdminAttempt, userContext, method, uri, referer, ip, response.getStatus(),
                        System.currentTimeMillis() - startTime);
            MDC.clear();
        }
    }

    private void logRequest(boolean adminAttempt, UserContext userContext, String method, String uri, String referer,
                            String ip, int status, long durationMs) {
        String safeReferer = referer != null ? referer : "-";

        if (adminAttempt) {
            if (userContext.authenticated()) {
                log.error(
                        "회원 관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 제공자: {}",
                        ip, safeReferer, uri, method, userContext.userId(), userContext.provider());
            } else {
                log.error("비회원 - 관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}", ip, safeReferer, uri,
                        method);
            }
        }

        if (userContext.authenticated()) {
            log.info("회원 요청 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, Status: {}, Duration: {}ms, 유저 ID: {}, 제공자: {}",
                    ip, safeReferer, uri, method, status, durationMs, userContext.userId(), userContext.provider());
        } else {
            log.info("비회원 요청 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, Status: {}, Duration: {}ms",
                    ip, safeReferer, uri, method, status, durationMs);
        }
    }

    /**
     * <h3>클라이언트 IP 주소 획득</h3>
     * <p>HTTP 요청에서 클라이언트의 실제 IP 주소를 추출합니다.</p>
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트의 IP 주소 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private void setMdcContext(UserContext userContext, String clientIp) {
        String existingTraceId = MDC.get("traceId");
        MDC.put("traceId", existingTraceId != null ? existingTraceId : generateTraceId());
        MDC.put("clientIp", clientIp);
        if (userContext.authenticated()) {
            MDC.put("userId", String.valueOf(userContext.userId()));
            MDC.put("username", userContext.username());
        } else {
            MDC.put("userId", "anonymous");
            MDC.put("username", "anonymous");
        }
    }

    private UserContext resolveUserContext(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return new UserContext(true, userDetails.getMemberId(), userDetails.getUsername(),
                    userDetails.getProvider().name());
        }
        return UserContext.anonymous();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record UserContext(boolean authenticated, Long userId, String username, String provider) {
        private static UserContext anonymous() {
            return new UserContext(false, null, "anonymous", null);
        }
    }
}
