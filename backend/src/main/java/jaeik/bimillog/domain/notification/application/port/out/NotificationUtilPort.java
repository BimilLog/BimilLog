package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.NotificationType;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>알림 관련 유틸리티 기능을 정의하는 Secondary Port</p>
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
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return Emitter ID
     */
    String makeTimeIncludeId(Long userId, Long tokenId);

    /**
     * <h3>알림 수신 자격 확인</h3>
     *
     * @param userId 사용자 ID
     * @param type   알림 타입
     * @return 알림 수신 가능 여부
     */
    boolean isEligibleForNotification(Long userId, NotificationType type);
}