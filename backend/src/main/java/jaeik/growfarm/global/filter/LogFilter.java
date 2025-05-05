package jaeik.growfarm.global.filter;

import jaeik.growfarm.global.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String referer = request.getHeader("Referer");

        if (uri.startsWith("/admin")) {
            String ip = getClientIp(request);
            String method = request.getMethod();

            // 로그인 사용자 ID 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                Long userId = userDetails.getUserDTO().getUserId();
                Long kakaoId = userDetails.getUserDTO().getKakaoId();
                String kakaoNickname = userDetails.getUserDTO().getKakaoNickname();
                log.warn("관리자 API 요청 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 카카오 ID: {}, 카카오 닉네임: {}", ip, referer, uri, method, userId, kakaoId, kakaoNickname);
            } else {
                log.warn("관리자 API 요청 - IP: {}, 오리진 URI: {}, 타겟 URI: {},Method: {}", ip, referer, uri, method);
            }

        }

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
