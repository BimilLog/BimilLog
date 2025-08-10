package jaeik.growfarm.global.auth;

import jaeik.growfarm.dto.user.ClientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>인증 쿠키 관리자</h2>
 *
 * <p>JWT 토큰을 사용하여 인증 관련 쿠키를 생성하고 관리합니다.</p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private final JwtHandler jwtHandler;

    public static final String ACCESS_TOKEN_COOKIE = "jwt_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "jwt_refresh_token";
    private static final int MAX_AGE = 3600;

    /**
     * <h3>JWT 토큰 쿠키 생성</h3>
     *
     * <p>Access 토큰과 Refresh 토큰이 담긴 쿠키 리스트를 생성한다</p>
     *
     * @param clientDTO 클라이언트용 DTO
     * @return 응답 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    public List<ResponseCookie> generateJwtCookie(ClientDTO clientDTO) {
        return List.of(generateJwtAccessCookie(clientDTO), generateJwtRefreshCookie(clientDTO));
    }

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     *
     * <p>로그아웃 시 사용되는 쿠키를 생성한다. 액세스 토큰과 리프레시 토큰을 모두 삭제합니다.</p>
     *
     * @return 로그아웃 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    public List<ResponseCookie> getLogoutCookies() {
        ResponseCookie accessTokenCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();

        return List.of(accessTokenCookie, refreshTokenCookie);
    }

    public ResponseCookie generateJwtAccessCookie(ClientDTO clientDTO) {
        String accessToken = jwtHandler.generateAccessToken(clientDTO);
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .path("/")
                .maxAge(MAX_AGE)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }

    private ResponseCookie generateJwtRefreshCookie(ClientDTO clientDTO) {
        String refreshToken = jwtHandler.generateRefreshToken(clientDTO);
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .path("/")
                .maxAge(MAX_AGE * 720L)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }
}
