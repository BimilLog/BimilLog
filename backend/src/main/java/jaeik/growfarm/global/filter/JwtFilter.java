package jaeik.growfarm.global.filter;

import jaeik.growfarm.dto.user.ClientDTO;
import jaeik.growfarm.entity.user.Token;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.auth.JwtTokenProvider;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import jaeik.growfarm.repository.token.TokenRepository;
import jaeik.growfarm.repository.user.UserRepository;
import jaeik.growfarm.service.auth.AuthService;
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
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

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
     * @since 1.0.9
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청은 JWT 필터를 거치지 않음
        if ("OPTIONS".equals(method)) {
            return true;
        }

        return path.equals("/") ||
                path.equals("/api/auth/login");
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
     * @since 1.0.0
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = extractTokenFromCookie(request, JwtTokenProvider.ACCESS_TOKEN_COOKIE);

        // Access Token이 유효할 때
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            setAuthentication(accessToken);
        } else { // accessToken이 없거나 유효 하지 않을 때
            String refreshToken = extractTokenFromCookie(request, JwtTokenProvider.REFRESH_TOKEN_COOKIE);
            // accessToken은 유효 하지 않지만 refreshToken은 유효할 때 accessToken 발급을 위해 refreshToken을
            // 검증
            if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                Long tokenId = jwtTokenProvider.getTokenIdFromToken(refreshToken);
                Long fcmTokenId = jwtTokenProvider.getFcmTokenIdFromToken(refreshToken);
                Token token = tokenRepository.findById(tokenId)
                        .orElseThrow(() -> new CustomException(ErrorCode.REPEAT_LOGIN));
                if (Objects.equals(token.getId(), tokenId)) {
                    // 카카오 토큰 갱신
                    authService.renewalKaKaoToken(token);

                    // 유저 정보 조회 (Setting 포함)
                    Users user = userRepository.findByIdWithSetting(token.getUsers().getId()).orElseThrow();
                    ClientDTO clientDTO = new ClientDTO(user, tokenId, fcmTokenId);

                    // 새로운 accessTokenCookie 발급
                    ResponseCookie cookie = jwtTokenProvider.generateJwtAccessCookie(clientDTO);
                    response.addHeader("Set-Cookie", cookie.toString());

                    // 사용자 인증 정보 설정
                    setAuthentication(cookie.getValue());
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
        ClientDTO clientDTO = jwtTokenProvider.getUserInfoFromToken(jwtAccessToken);
        CustomUserDetails customUserDetails = new CustomUserDetails(clientDTO);
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