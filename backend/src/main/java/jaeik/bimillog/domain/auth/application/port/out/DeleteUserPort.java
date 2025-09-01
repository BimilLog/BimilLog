package jaeik.bimillog.domain.auth.application.port.out;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>사용자 삭제 포트</h2>
 * <p>사용자 로그아웃 및 탈퇴 처리를 위한 아웃바운드 포트</p>
 * <p>헥사고날 아키텍처에서 인프라스트럭처 계층의 사용자 삭제 기능을 추상화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteUserPort {

    /**
     * <h3>로그아웃 처리</h3>
     * <p>다중 로그인 환경에서 특정 토큰만 삭제하여 해당 기기만 로그아웃 처리합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 삭제할 토큰 ID (null인 경우 모든 토큰 삭제 - 회원탈퇴용)
     * @since 2.0.0
     * @author Jaeik
     */
    void logoutUser(Long userId, Long tokenId);

    /**
     * <h3>회원탈퇴 처리</h3>
     *
     * @param userId 사용자 ID
     */
    void performWithdrawProcess(Long userId);

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     * <p>사용자 로그아웃 시 JWT 토큰을 무효화하는 쿠키를 생성</p>
     *
     * @return 로그아웃 쿠키 리스트 (Access Token, Refresh Token 무효화 쿠키)
     * @since 2.0.0
     * @author Jaeik
     */
    List<ResponseCookie> getLogoutCookies();
}
