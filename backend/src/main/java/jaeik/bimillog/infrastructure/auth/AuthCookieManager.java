package jaeik.bimillog.infrastructure.auth;

import jaeik.bimillog.infrastructure.adapter.user.in.web.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.List;

// 로컬 테스트를 위해 쿠키의 HTTPS해제
/**
 * <h2>인증 쿠키 관리자</h2>
 *
 * <p>JWT 토큰을 사용하여 인증 관련 쿠키를 생성하고 관리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private final JwtHandler jwtHandler;

    public static final String ACCESS_TOKEN_COOKIE = "jwt_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "jwt_refresh_token";
    public static final String TEMP_USER_ID_COOKIE = "temp_user_id";
    private static final int MAX_AGE = 3600;

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     *
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성합니다.</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    public ResponseCookie createTempCookie(String uuid) {
        return ResponseCookie.from(TEMP_USER_ID_COOKIE, uuid)
                .path("/")
                .maxAge(600) // 10분
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();
    }

    /**
     * <h3>JWT 토큰 쿠키 생성</h3>
     *
     * <p>Access 토큰과 Refresh 토큰이 담긴 쿠키 리스트를 생성한다</p>
     *
     * @param userDTO 클라이언트용 DTO
     * @return 응답 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    public List<ResponseCookie> generateJwtCookie(UserDTO userDTO) {
        return List.of(generateJwtAccessCookie(userDTO), generateJwtRefreshCookie(userDTO));
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
                .secure(false)
                .build();

        ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();

        return List.of(accessTokenCookie, refreshTokenCookie);
    }

    /**
     * <h3>JWT 액세스 토큰 쿠키 생성</h3>
     * <p>주어진 UserDTO를 기반으로 JWT 액세스 토큰 쿠키를 생성합니다.</p>
     *
     * @param userDTO 사용자 정보 DTO
     * @return 생성된 액세스 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    public ResponseCookie generateJwtAccessCookie(UserDTO userDTO) {
        String accessToken = jwtHandler.generateAccessToken(userDTO);
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .path("/")
                .maxAge(MAX_AGE)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();
    }

    /**
     * <h3>JWT 리프레시 토큰 쿠키 생성</h3>
     * <p>주어진 UserDTO를 기반으로 JWT 리프레시 토큰 쿠키를 생성합니다.</p>
     *
     * @param userDTO 사용자 정보 DTO
     * @return 생성된 리프레시 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    public ResponseCookie generateJwtRefreshCookie(UserDTO userDTO) {
        String refreshToken = jwtHandler.generateRefreshToken(userDTO);
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .path("/")
                .maxAge(MAX_AGE * 720L)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(false)
                .build();
    }
}
