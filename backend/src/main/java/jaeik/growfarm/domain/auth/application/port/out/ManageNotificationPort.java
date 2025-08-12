package jaeik.growfarm.domain.auth.application.port.out;

/**
 * <h2>알림 관리 포트</h2>
 * <p>SSE 연결 관리를 위한 포트</p>
 * <p>책임 분리 원칙에 따라 알림 관련 작업만을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface ManageNotificationPort {

    /**
     * <h3>SSE 연결 삭제</h3>
     * <p>사용자 로그아웃 시 해당 사용자의 모든 SSE 연결을 정리</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);
}