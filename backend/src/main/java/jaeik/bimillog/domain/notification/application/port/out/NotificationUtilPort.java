package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.FcmToken;
import jaeik.bimillog.domain.notification.entity.NotificationType;

import java.util.List;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>알림 관련 유틸리티 기능을 정의하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationUtilPort {

    /**
     * <h3>고유 Emitter ID 생성</h3>
     * <p>타임스탬프를 포함한 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return Emitter ID
     * @author Jaeik
     * @since 2.0.0
     */
    String makeTimeIncludeId(Long userId, Long tokenId);

    /**
     * <h3>알림 수신 자격 확인</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는지 확인합니다.</p>
     *
     * @param userId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 가능 여부
     * @author Jaeik
     * @since 2.0.0
     */
    boolean SseEligibleForNotification(Long userId, NotificationType type);

    /**
     * <h3>알림 수신 자격이 있는 FCM 토큰 조회</h3>
     * <p>사용자가 특정 타입의 알림을 받을 수 있는 경우 해당 사용자의 모든 FCM 토큰을 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 자격이 있는 경우 FCM 토큰 목록, 없는 경우 빈 목록
     * @author Jaeik
     * @since 2.0.0
     */
    List<FcmToken> FcmEligibleFcmTokens(Long userId, NotificationType type);
}