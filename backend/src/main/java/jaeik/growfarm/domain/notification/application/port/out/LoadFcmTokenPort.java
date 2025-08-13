package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.entity.FcmToken;
import java.util.List;

/**
 * <h2>FCM 토큰 조회 Port</h2>
 * <p>사용자의 유효한 FCM 토큰을 조회하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadFcmTokenPort {
    /**
     * <h3>사용자 ID로 유효한 FCM 토큰 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 유효한 FCM 토큰 목록을 조회합니다.</p>
     *
     * @param userId 조회할 사용자의 ID
     * @return FCM 토큰 엔티티 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<FcmToken> findValidFcmTokensByUserId(Long userId);
}
