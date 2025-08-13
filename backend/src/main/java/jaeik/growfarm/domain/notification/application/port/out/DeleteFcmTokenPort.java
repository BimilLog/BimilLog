package jaeik.growfarm.domain.notification.application.port.out;

/**
 * <h2>FCM 토큰 삭제 Port</h2>
 * <p>FCM 토큰을 삭제하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface DeleteFcmTokenPort {
    
    /**
     * <h3>사용자 ID로 FCM 토큰 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 FCM 토큰을 삭제합니다.</p>
     *
     * @param userId 삭제할 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByUserId(Long userId);
}