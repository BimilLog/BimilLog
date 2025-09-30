package jaeik.bimillog.infrastructure.filter;

import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
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

/**
 * <h2>로그 필터</h2>
 * <p>
 * HTTP 요청에 대한 로그를 기록하는 필터 클래스
 * </p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Component
public class LogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WHITELIST = List.of("/api/auth/me", "/api/auth/health", "/api/comment/{postId}",
            "/api/notification/subscribe",
            "/api/paper", "/api/post/query", "api/post/query/{postId}", "/api/post/search", "api/post/manage/write",
            "/api/post/manage/update", "/api/post/manage/delete",
            "/api/post/cache/realtime", "/api/post/cache/weekly", "/api/post/cache/legend", "/api/post/cache/notice",
            "/api/member/posts", "/api/member/comments", "/api/member/likeposts", "/api/member/likecomments",
            "/api/member/username/check");

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

        String referer = request.getHeader("Referer");
        String ip = getClientIp(request);
        String method = request.getMethod();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getExistingMemberDetail().getUserId();
            String socialId = userDetails.getExistingMemberDetail().getSocialId();
            String socialNickname = userDetails.getExistingMemberDetail().getSocialNickname();
            String provider = userDetails.getExistingMemberDetail().getProvider().name();

            if (uri.startsWith("/dto")) {
                log.error(
                        "회원 관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 제공자: {}, 소셜 ID: {}, 소셜 닉네임: {}",
                        ip, referer, uri, method, userId, provider, socialId, socialNickname);
            }
            log.info("회원 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}, 유저 ID: {}, 제공자: {}, 소셜 ID: {}, 소셜 닉네임: {}", ip,
                    referer, uri, method, userId, provider, socialId, socialNickname);
        } else {
            if (uri.startsWith("/dto")) {
                log.error("비회원 - 관리자 페이지 접근 시도 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}", ip, referer, uri,
                        method);
            }
            log.info("비회원 - IP: {}, 오리진 URI: {}, 타겟 URI: {}, Method: {}", ip, referer, uri, method);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * <h3>화이트리스트 경로 확인</h3>
     * <p>주어진 URI가 로그 필터 화이트리스트에 포함되는지 확인합니다.</p>
     *
     * @param uri 확인할 URI 문자열
     * @return 화이트리스트에 포함되면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    private boolean isWhitelisted(String uri) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
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
}
