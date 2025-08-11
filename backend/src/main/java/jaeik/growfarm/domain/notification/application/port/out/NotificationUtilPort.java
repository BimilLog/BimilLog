package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.domain.NotificationType;
import jaeik.growfarm.dto.notification.EventDTO;

/**
 * <h2>알림 유틸리티 포트</h2>
 * <p>알림 이벤트 DTO 생성 등 유틸리티 기능을 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationUtilPort {

    /**
     * <h3>이벤트 DTO 생성</h3>
     *
     * @param type    알림 타입
     * @param message 알림 메시지
     * @param url     알림 URL
     * @return 이벤트 DTO
     */
    EventDTO createEventDTO(NotificationType type, String message, String url);

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