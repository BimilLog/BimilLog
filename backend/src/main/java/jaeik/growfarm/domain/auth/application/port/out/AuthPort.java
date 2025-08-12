package jaeik.growfarm.domain.auth.application.port.out;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>인증 포트</h2>
 * <p>인증과 관련된 쿠키 관리를 위한 포트</p>
 * <p>책임 분리 원칙에 따라 인증 관련 작업만을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public interface AuthPort {

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     * <p>사용자 로그아웃 시 JWT 토큰을 무효화하는 쿠키를 생성</p>
     *
     * @return 로그아웃 쿠키 리스트 (Access Token, Refresh Token 무효화 쿠키)
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> getLogoutCookies();

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성</p>
     *
     * @param uuid 임시 사용자 ID
     * @return 임시 사용자 ID 쿠키
     * @since 2.0.0
     * @author Jaeik
     */
    ResponseCookie createTempCookie(String uuid);
}