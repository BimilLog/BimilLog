package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.entity.NotificationEvent;

/**
 * <h2>알림 전송 인터페이스</h2>
 * <p>
 * 다양한 알림 전송 방식을 추상화하는 인터페이스
 * </p>
 * <p>
 * 헥사고날 아키텍처에 따라 도메인 엔티티인 NotificationEvent를 사용하여
 * 인프라 세부사항에 의존하지 않는 순수한 포트 계약을 제공합니다.
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
     * @param userId 사용자 ID
     * @param event  알림 이벤트 (도메인 엔티티)
     * @author Jaeik
     * @since 2.0.0
     */
    void send(Long userId, NotificationEvent event);
}