package jaeik.bimillog.infrastructure.filter;

import jaeik.bimillog.domain.auth.entity.AuthToken;
import jaeik.bimillog.domain.auth.service.BlacklistService;
import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenQueryAdapter;
import jaeik.bimillog.domain.global.out.GlobalAuthTokenSaveAdapter;
import jaeik.bimillog.domain.global.out.GlobalCookieAdapter;
import jaeik.bimillog.domain.global.out.GlobalJwtAdapter;
import jaeik.bimillog.domain.member.entity.Member;
import jaeik.bimillog.domain.member.out.MemberQueryAdapter;
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

/**
 * <h2>JWT 필터</h2>
 * <p>
 * JWT 토큰을 검증하고 인증 정보를 설정하는 필터
 * </p>
 *
 * @author Jaeik
 * @version 1.0.9
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final GlobalAuthTokenQueryAdapter globalAuthTokenQueryAdapter;
    private final MemberQueryAdapter memberQueryAdapter;
    private final GlobalJwtAdapter globalJwtAdapter;
    private final GlobalCookieAdapter globalCookieAdapter;
    private final BlacklistService blacklistService;
    private final GlobalAuthTokenSaveAdapter globalAuthTokenSaveAdapter;

    /**
     * <h3>필터 제외 경로 설정</h3>
     * <p>
     * JWT 필터를 적용하지 않을 경로들을 설정합니다.
     * permitAll()로 설정된 인증이 필요없는 경로들은 JWT 필터를 거치지 않도록 합니다.
     * </p>
     *
     * @param request HTTP 요청 객체
     * @return 필터를 적용하지 않을 경우 true, 적용할 경우 false
     * @author Jaeik
     * @since 2.0.0
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
     * <p>
     * Security Filter Chain중 사용자인증필터앞에 삽입되어 JWT토큰에 관한 처리를 하는 필터입니다.
     * </p>
     * <p>
     * 로그인이 필요한 요청은 엑세스 토큰과 리프레시 토큰이 모두 유효한 경우만 통과 가능합니다.
     * </p>
     * <p>
     * 엑세스 토큰이 유효하지 않은 경우 리프레시 토큰을 검증하여 새로운 엑세스 토큰을 발급합니다.
     * </p>
     * <p>
     * 리프레시 토큰이 유효하지 않는 경우 401에러가 반환되며 재 로그인을 해야합니다.
     * </p>
     *
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException      입출력 예외
     * @author Jaeik
     * @since 1.0.20
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = extractTokenFromCookie(request, "jwt_access_token");

        // 1. Access Token이 유효하고 블랙리스트에 없을 때
        if (accessToken != null && globalJwtAdapter.validateToken(accessToken) && !blacklistService.isBlacklisted(accessToken)) {
            setAuthentication(accessToken);
        } else {
            // 2. Access Token이 만료되었을 때 리프레시 플로우
            String refreshToken = extractTokenFromCookie(request, "jwt_refresh_token");

            // 2-1. 리프레시 토큰 존재 여부 및 JWT 유효성 검증
            if (refreshToken == null || !globalJwtAdapter.validateToken(refreshToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2-2. 블랙리스트 확인
            if (blacklistService.isBlacklisted(refreshToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // 2-3. 리프레시 토큰에서 authTokenId 추출
                Long tokenId = globalJwtAdapter.getTokenIdFromToken(refreshToken);

                // 2-4. DB에서 AuthToken 엔티티 조회
                AuthToken authToken = globalAuthTokenQueryAdapter.findById(tokenId)
                        .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));

                // 2-5. DB 저장 토큰과 클라이언트 토큰 비교 검증
                if (!refreshToken.equals(authToken.getRefreshToken())) {
                    throw new CustomException(ErrorCode.TOKEN_MISMATCH);
                }

                // 2-6. 유저 정보 조회
                Member member = memberQueryAdapter.findByIdWithSetting(authToken.getMember().getId())
                        .orElseThrow(() -> new CustomException(ErrorCode.TOKEN_NOT_FOUND));
                CustomUserDetails userDetails = CustomUserDetails.ofExisting(member, tokenId);

                // 2-7. 새 액세스 토큰 발급
                String newAccessToken = globalJwtAdapter.generateAccessToken(userDetails);
                ResponseCookie accessCookie = globalCookieAdapter.generateJwtAccessCookie(newAccessToken);
                response.setHeader("Set-Cookie", accessCookie.toString());

                // 2-8. Refresh AuthToken Rotation (15일 이하 남았을 때)
                if (globalJwtAdapter.shouldRefreshToken(refreshToken, 15)) {
                    String newRefreshToken = globalJwtAdapter.generateRefreshToken(userDetails);

                    // DB 업데이트
                    globalAuthTokenSaveAdapter.updateJwtRefreshToken(tokenId, newRefreshToken);

                    // 새 리프레시 토큰 쿠키 발급
                    ResponseCookie refreshCookie = globalCookieAdapter.generateJwtRefreshCookie(newRefreshToken);
                    response.addHeader("Set-Cookie", refreshCookie.toString());
                }

                // 2-9. 인증 정보 설정
                setAuthentication(newAccessToken);

            } catch (CustomException e) {
                // 보안 예외 발생 시 필터 체인 중단
                SecurityContextHolder.clearContext(); // 인증 정보 초기화
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\": \"" + e.getErrorCode().name() + "\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
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
        CustomUserDetails userDetails = globalJwtAdapter.getUserInfoFromToken(jwtAccessToken);
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