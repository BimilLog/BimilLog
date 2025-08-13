package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.entity.FcmToken;

/**
 * <h2>FCM 토큰 저장 Port</h2>
 * <p>FCM 토큰을 저장하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SaveFcmTokenPort {
    
    /**
     * <h3>FCM 토큰 저장</h3>
     * <p>FCM 토큰 엔티티를 저장합니다.</p>
     *
     * @param fcmToken 저장할 FCM 토큰 엔티티
     * @return 저장된 FCM 토큰 엔티티
     * @author Jaeik
     * @since 2.0.0
     */
    FcmToken save(FcmToken fcmToken);
}