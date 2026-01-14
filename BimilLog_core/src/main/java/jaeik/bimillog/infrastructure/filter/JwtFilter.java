package jaeik.bimillog.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.service.BlacklistService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.infrastructure.adapter.AuthTokenAdapter;
import jaeik.bimillog.infrastructure.exception.ErrorResponse;
import jaeik.bimillog.infrastructure.web.HTTPCookie;
import jaeik.bimillog.infrastructure.web.JwtFactory;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.repository.MemberRepository;
import jaeik.bimillog.infrastructure.exception.CustomException;
import jaeik.bimillog.infrastructure.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * <h2>JWT 필터</h2>
 * <p>JWT 토큰을 검증하고 인증 정보를 설정하는 필터</p>
 *
 * @author Jaeik
 * @version 2.5.0
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final AuthTokenAdapter authTokenAdapter;
    private final MemberRepository memberRepository;
    private final JwtFactory jwtFactory;
    private final HTTPCookie HTTPCookie;
    private final BlacklistService blacklistService;
    private final ObjectMapper objectMapper;

    /**
     * <h3>필터 제외 경로 설정</h3>
     * <p>
     * JWT 필터를 적용하지 않을 경로들을 설정합니다.
     * permitAll()로 설정된 인증이 필요없는 경로들은 JWT 필터를 거치지 않도록 합니다.
     * </p>
     *
     * @param request HTTP 요청 객체
     * @return 필터를 적용하지 않을 경우 true, 적용할 경우 false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청은 JWT 필터를 거치지 않음
        if ("OPTIONS".equals(method)) {
            return true;
        }

        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/signup") ||
                path.equals("/api/global/health") ||
                path.equals("/api/member/username/check") ||
                path.equals("/api/member/suggestion") ||
                path.equals("/api/member/report");
    }

    /**
     * <h3>필터 내부 처리</h3>
     *
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException      입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = extractTokenFromCookie(request, "jwt_access_token");
        String refreshToken = extractTokenFromCookie(request, "jwt_refresh_token");

        // 리프레시 토큰 블랙리스트 검사
        if (blacklistService.isBlacklisted(refreshToken)) {
            setErrorResponse(response, "정지되거나 차단된 회원입니다.");
            return;
        }

        // 리프레시 토큰 미유효
        if (!jwtFactory.validateToken(refreshToken)) {
            setErrorResponse(response, "다시 로그인 해주세요.");
            return;
        }

        // 액세스 토큰 유효
        if (jwtFactory.validateToken(accessToken)) {
            setAuthentication(accessToken);
            filterChain.doFilter(request, response);
            return;
        }

        // 액세스 토큰 미유효 그리고 리프레시 토큰 유효만 남음
        // 리프레시 토큰에서 authTokenId 추출
        Long authTokenId = jwtFactory.getTokenIdFromToken(refreshToken);

        // DB에서 AuthToken 엔티티 조회
        Optional<AuthToken> optionalAuthToken = authTokenAdapter.findById(authTokenId);

        if (optionalAuthToken.isEmpty()) {
            setErrorResponse(response, "토큰 정보를 조회할 수 없습니다.");
            return;
        }

        AuthToken authToken = optionalAuthToken.get();

        // DB 저장 토큰과 클라이언트 토큰 비교 검증
        if (!refreshToken.equals(authToken.getRefreshToken())) {
            setErrorResponse(response, "다시 로그인 해주세요.");
            return;
        }

        // 유저 정보 조회
        Optional<Member> optionalMember = memberRepository.findByIdWithSetting(authToken.getMember().getId());

        if (optionalMember.isEmpty()) {
            setErrorResponse(response, "유저 정보를 조회할 수 없습니다.");
            return;
        }

        Member member = optionalMember.get();
        CustomUserDetails userDetails = CustomUserDetails.ofExisting(member, authTokenId);

        // 새 액세스 토큰 발급
        String newAccessToken = jwtFactory.generateAccessToken(userDetails);
        ResponseCookie accessCookie = HTTPCookie.generateJwtAccessCookie(newAccessToken);
        response.setHeader("Set-Cookie", accessCookie.toString());

        // 리프레시토큰 로테이션
        rotateRefreshToken(response, refreshToken, userDetails, authTokenId);

        // 인증 정보 설정
        setAuthentication(newAccessToken);
        filterChain.doFilter(request, response);
    }

    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED,
                message
        );
        String result = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(result);
    }

    // RefreshToken Rotation (15일 이하 남았을 때)
    private void rotateRefreshToken(HttpServletResponse response, String refreshToken, CustomUserDetails userDetails, Long authTokenId) {
        if (jwtFactory.shouldRefreshToken(refreshToken, 15)) {
            String newRefreshToken = jwtFactory.generateRefreshToken(userDetails);

            // DB 업데이트
            authTokenAdapter.updateJwtRefreshToken(authTokenId, newRefreshToken);

            // 새 리프레시 토큰 쿠키 발급
            ResponseCookie refreshCookie = HTTPCookie.generateJwtRefreshCookie(newRefreshToken);
            response.addHeader("Set-Cookie", refreshCookie.toString());
        }
    }

    /**
     * <h3>인증 정보 설정</h3>
     * <p>
     * JWT 엑세스 토큰 에서 사용자 정보를 추출하여 인증 정보를 설정합니다.
     * </p>
     *
     * @param jwtAccessToken JWT 엑세스 토큰
     */
    private void setAuthentication(String jwtAccessToken) {
        CustomUserDetails userDetails = jwtFactory.getUserInfoFromToken(jwtAccessToken);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * <h3>쿠키에서 토큰 추출</h3>
     * <p>
     * HTTP 요청에서 지정된 쿠키 이름의 값을 추출합니다.
     * </p>
     *
     * @param request    HTTP 요청 객체
     * @param cookieName 쿠키 이름
     * @return 쿠키 값, 없으면 null
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}