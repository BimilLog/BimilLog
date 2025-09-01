package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.NotificationType;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>알림 관련 유틸리티 기능을 정의하는 아웃바운드 포트</p>
 * <p>
 * 헥사고날 아키텍처 리팩토링으로 EventDTO 생성 기능을 제거하고,
 * 도메인 서비스에서 직접 NotificationEvent.create()를 사용하도록 변경되었습니다.
 * </p>
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
    boolean isEligibleForNotification(Long userId, NotificationType type);
}