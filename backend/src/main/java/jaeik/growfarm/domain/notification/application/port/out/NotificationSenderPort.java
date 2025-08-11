package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.EventDTO;

/**
 * <h2>알림 전송 포트</h2>
 * <p>다양한 알림 전송 방식에 대한 인프라 액세스를 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSenderPort {

    /**
     * <h3>모든 알림 방식으로 전송</h3>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보
     */
    void sendNotificationToAll(Long userId, EventDTO eventDTO);
}