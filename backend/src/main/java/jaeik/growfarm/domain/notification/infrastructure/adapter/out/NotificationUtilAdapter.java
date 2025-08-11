package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.service.notification.NotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <h2>알림 유틸리티 어댑터</h2>
 * <p>알림 이벤트 DTO 생성 등 유틸리티 기능을 제공하는 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationUtilAdapter implements NotificationUtilPort {

    private final NotificationUtil notificationUtil;

    @Override
    public EventDTO createEventDTO(NotificationType type, String message, String url) {
        return notificationUtil.createEventDTO(type, message, url);
    }

    @Override
    public String makeTimeIncludeId(Long userId, Long tokenId) {
        return notificationUtil.makeTimeIncludeId(userId, tokenId);
    }
}