package jaeik.bimillog.infrastructure.adapter.out.global;

import jaeik.bimillog.domain.global.application.port.out.GlobalCookiePort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

// 로컬 테스트를 위해 쿠키의 HTTPS해제
/**
 * <h2>공용 쿠키 어댑터</h2>
 *
 * <p>JWT 토큰을 사용하여 인증 관련 쿠키를 생성하고 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class GlobalCookieAdapter implements GlobalCookiePort {
    public static final String ACCESS_TOKEN_COOKIE = "jwt_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "jwt_refresh_token";
    public static final String TEMP_USER_ID_COOKIE = "temp_user_id";
    private static final int MAX_AGE = 3600;

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     *
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성합니다.</p>
     *
     * @param uuid 임시 사용자 UUID
     * @return 임시 사용자 ID 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ResponseCookie createTempCookie(String uuid) {
        return ResponseCookie.from(TEMP_USER_ID_COOKIE, uuid)
                .path("/")
                .maxAge(600) // 10분
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }

    /**
     * <h3>JWT 토큰 쿠키 생성</h3>
     *
     * <p>Access 토큰과 Refresh 토큰이 담긴 쿠키 리스트를 생성한다</p>
     *
     * @param accessToken JWT 액세스 토큰
     * @param refreshToken JWT 리프레시 토큰
     * @return 응답 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public List<ResponseCookie> generateJwtCookie(String accessToken, String refreshToken) {
        return List.of(generateJwtAccessCookie(accessToken), generateJwtRefreshCookie(refreshToken));
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
    @Override
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

    /**
     * <h3>JWT 액세스 토큰 쿠키 생성</h3>
     * <p>주어진 JWT 액세스 토큰으로 쿠키를 생성합니다.</p>
     *
     * @param accessToken JWT 액세스 토큰
     * @return 생성된 액세스 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ResponseCookie generateJwtAccessCookie(String accessToken) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .path("/")
                .maxAge(MAX_AGE)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }

    /**
     * <h3>JWT 리프레시 토큰 쿠키 생성</h3>
     * <p>주어진 JWT 리프레시 토큰으로 쿠키를 생성합니다.</p>
     *
     * @param refreshToken JWT 리프레시 토큰
     * @return 생성된 리프레시 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public ResponseCookie generateJwtRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .path("/")
                .maxAge(MAX_AGE * 720L)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }

    /**
     * <h3>임시 사용자 ID 쿠키 만료</h3>
     * <p>temp_user_id 쿠키를 즉시 만료시키는 ResponseCookie를 생성합니다.</p>
     *
     * @return 만료 처리된 임시 쿠키 ResponseCookie
     */
    @Override
    public ResponseCookie expireTempCookie() {
        return ResponseCookie.from(TEMP_USER_ID_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(true)
                .build();
    }
}
