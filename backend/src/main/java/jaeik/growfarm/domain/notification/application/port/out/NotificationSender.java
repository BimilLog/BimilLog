package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.EventDTO;

/**
 * <h2>알림 전송 인터페이스</h2>
 * <p>
 * 다양한 알림 전송 방식을 추상화하는 인터페이스
 * </p>
 * <p>

 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSender {

    /**
     * <h3>알림 전송</h3>
     * <p>
     * 사용자에게 알림을 전송합니다.
     * </p>
     *
     * @param userId   사용자 ID
     * @param eventDTO 이벤트 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    void send(Long userId, EventDTO eventDTO);
}