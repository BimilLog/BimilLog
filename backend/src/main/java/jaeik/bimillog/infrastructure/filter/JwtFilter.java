package jaeik.bimillog.infrastructure.filter;

import jaeik.bimillog.domain.auth.application.port.in.TokenBlacklistUseCase;
import jaeik.bimillog.domain.user.application.port.out.TokenPort;
import jaeik.bimillog.domain.user.application.port.out.UserQueryPort;
import jaeik.bimillog.domain.user.entity.Token;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.infrastructure.adapter.user.in.web.UserDTO;
import jaeik.bimillog.infrastructure.auth.AuthCookieManager;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jaeik.bimillog.infrastructure.auth.JwtHandler;
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
import java.util.Objects;

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
    private final TokenPort tokenPort;
    private final UserQueryPort userQueryPort;
    private final JwtHandler jwtHandler;
    private final AuthCookieManager authCookieManager;
    private final TokenBlacklistUseCase tokenBlacklistUseCase;

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

        return path.equals("/api/auth/login, /api/auth/signup, /api/auth/health");
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

        String accessToken = extractTokenFromCookie(request, AuthCookieManager.ACCESS_TOKEN_COOKIE);

        // Access Token이 유효하고 블랙리스트에 없을 때
        if (accessToken != null && jwtHandler.validateToken(accessToken) && !tokenBlacklistUseCase.isBlacklisted(accessToken)) {
            setAuthentication(accessToken);
        } else { // accessToken이 없거나 유효하지 않거나 블랙리스트에 있을 때
            String refreshToken = extractTokenFromCookie(request, AuthCookieManager.REFRESH_TOKEN_COOKIE);
            // accessToken은 유효하지 않지만 refreshToken은 유효하고 블랙리스트에 없을 때 accessToken 발급을 위해 refreshToken을 검증
            if (refreshToken != null && jwtHandler.validateToken(refreshToken) && !tokenBlacklistUseCase.isBlacklisted(refreshToken)) {
                Long tokenId = jwtHandler.getTokenIdFromToken(refreshToken);
                // fcmTokenId 제거 - 이벤트 기반 방식으로 변경
                Token token = tokenPort.findById(tokenId)
                        .orElseThrow(() -> new CustomException(ErrorCode.REPEAT_LOGIN));
                if (Objects.equals(token.getId(), tokenId)) {

                    // 유저 정보 조회 (Setting 포함)
                    User user = userQueryPort.findByIdWithSetting(token.getUsers().getId()).orElseThrow();
                    UserDTO userDTO = UserDTO.of(user, tokenId, null); // fcmTokenId는 null로 설정

                    // 새로운 accessTokenCookie 발급
                    ResponseCookie accessCookie = authCookieManager.generateJwtAccessCookie(userDTO);
                    response.addHeader("Set-Cookie", accessCookie.toString());

                    // 리프레시 토큰이 15일 이하로 남았으면 새로운 리프레시 토큰도 발급
                    if (jwtHandler.shouldRefreshToken(refreshToken, 15)) {
                        ResponseCookie refreshCookie = authCookieManager.generateJwtRefreshCookie(userDTO);
                        response.addHeader("Set-Cookie", refreshCookie.toString());
                    }

                    // 사용자 인증 정보 설정
                    setAuthentication(accessCookie.getValue());
                }
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
        UserDTO userDTO = jwtHandler.getUserInfoFromToken(jwtAccessToken);
        CustomUserDetails customUserDetails = new CustomUserDetails(userDTO);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities());

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