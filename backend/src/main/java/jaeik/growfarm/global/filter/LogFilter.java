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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class LogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    private static final List<String> WHITELIST = List.of("/", "/board/", "/board/realtime", "/board/weekly",
            "/board/fame", "/board/search", "/board/{postId}", "/farm/{farmName}", "/auth/login", "/auth/signUp", "/auth/me", "/d fauth/health");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (isWhitelisted(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String referer = request.getHeader("Referer");
        String ip = getClientIp(request);
        String method = request.getMethod();

        // 로그인 사용자 ID 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getUserDTO().getUserId();
            Long kakaoId = userDetails.getUserDTO().getKakaoId();
            String kakaoNickname = userDetails.getUserDTO().getKakaoNickname();

            if (uri.startsWith("/admin")) {
                log.error("관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 카카오 ID: {}, 카카오 닉네임: {}", ip, referer, uri, method, userId, kakaoId, kakaoNickname);
            }
            log.info("IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 카카오 ID: {}, 카카오 닉네임: {}", ip, referer, uri, method, userId, kakaoId, kakaoNickname);
        } else {
            if (uri.startsWith("/admin")) {
                log.error("관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}", ip, referer, uri, method);
            }
            log.error("미 인증자 서버 직접 접근 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}", ip, referer, uri, method);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
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
