package jaeik.growfarm.domain.auth.application.port.out;

import org.springframework.http.ResponseCookie;

import java.util.List;

/**
 * <h2>알림 관리 포트</h2>
 * <p>SSE 연결 및 쿠키 관리를 위한 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManageNotificationPort {

    /**
     * <h3>SSE 연결 삭제</h3>
     *
     * @param userId 사용자 ID
     */
    void deleteAllEmitterByUserId(Long userId);

    /**
     * <h3>로그아웃 쿠키 생성</h3>
     *
     * @return 로그아웃 쿠키 리스트
     */
    List<ResponseCookie> getLogoutCookies();
}