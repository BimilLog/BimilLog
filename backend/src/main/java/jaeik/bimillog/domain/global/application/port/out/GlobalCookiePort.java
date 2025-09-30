package jaeik.bimillog.domain.global.application.port.out;

import jaeik.bimillog.domain.member.entity.memberdetail.NewMemberDetail;
import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>공용 쿠키 포트</h2>
 *
 * <p>JWT 토큰을 사용하여 인증 관련 쿠키를 생성하고 관리합니다.</p>
 * <p>GlobalCookieAdapter를 추상화 합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface GlobalCookiePort {

    /**
     * <h3>임시 사용자 ID 쿠키 생성</h3>
     *
     * <p>신규 회원가입 시 사용자의 임시 UUID를 담는 쿠키를 생성합니다.</p>
     *
     * @param newMemberDetail 임시 사용자 정보
     * @return 임시 사용자 ID 쿠키
     * @author Jaeik
     * @since 2.0.0
     */
    ResponseCookie createTempCookie(NewMemberDetail newMemberDetail);

    /**
     * <h3>JWT 토큰 쿠키 생성</h3>
     *
     * <p>Access 토큰과 Refresh 토큰이 담긴 쿠키 리스트를 생성한다</p>
     *
     * @param userDetail 클라이언트용 DTO
     * @return 응답 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> generateJwtCookie(String accessToken, String refreshToken);

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     *
     * <p>로그아웃 시 사용되는 쿠키를 생성한다. 액세스 토큰과 리프레시 토큰을 모두 삭제합니다.</p>
     *
     * @return 로그아웃 쿠키 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<ResponseCookie> getLogoutCookies();

    /**
     * <h3>JWT 액세스 토큰 쿠키 생성</h3>
     * <p>주어진 UserDTO를 기반으로 JWT 액세스 토큰 쿠키를 생성합니다.</p>
     *
     * @param userDetail 사용자 정보 DTO
     * @return 생성된 액세스 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    ResponseCookie generateJwtAccessCookie(String accessToken);

    /**
     * <h3>JWT 리프레시 토큰 쿠키 생성</h3>
     * <p>주어진 UserDTO를 기반으로 JWT 리프레시 토큰 쿠키를 생성합니다.</p>
     *
     * @param userDetail 사용자 정보 DTO
     * @return 생성된 리프레시 토큰 ResponseCookie
     * @author Jaeik
     * @since 2.0.0
     */
    ResponseCookie generateJwtRefreshCookie(String refreshToken);
}
