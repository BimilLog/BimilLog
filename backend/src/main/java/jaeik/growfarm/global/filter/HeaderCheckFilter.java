package jaeik.growfarm.global.filter;

import jaeik.growfarm.global.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * <h2>헤더 체크 필터</h2>
 * <p>특정 헤더 값이 일치하는지 확인하고, 일치하지 않을 경우 접근을 차단하는 필터</p>
 * <p>신뢰할 수 있는 프론트엔드에서만 접근을 허용</p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Component
public class HeaderCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeaderCheckFilter.class);

    private static final String TRUSTED_HEADER_NAME = "X-Frontend-Identifier";

    @Value("${secret-Identifier}")
    private String secretIdentifier;

    private static final List<String> WHITELIST = List.of("notification/subscribe", "/auth/health");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String headerValue = request.getHeader(TRUSTED_HEADER_NAME);

        String uri = request.getRequestURI();

        if (WHITELIST.contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!secretIdentifier.equals(headerValue)) {
            String ip = getClientIp(request);
            String method = request.getMethod();
            String referer = request.getHeader("Referer");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Long userId = userDetails.getClientDTO().getUserId();
                Long kakaoId = userDetails.getClientDTO().getKakaoId();
                String kakaoNickname = userDetails.getClientDTO().getKakaoNickname();
                log.error("비정상적인 접근 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 카카오 ID: {}, 카카오 닉네임: {}", ip, referer, uri, method, userId, kakaoId, kakaoNickname);
            } else {
                log.error("비정상적인 접근 - IP: {}, 오리진 URI: {}, 타겟 URI: {},Method: {}", ip, referer, uri, method);
            }

            response.sendError(HttpServletResponse.SC_FORBIDDEN, "비정상 접근 차단");
            return;
        }

        // 정상 접근은 필터 체인 계속 진행
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0];
        }
        return ip;
    }
}
