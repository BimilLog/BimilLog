package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationQueryUseCase;
import jaeik.growfarm.domain.notification.application.port.out.LoadNotificationPort;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>알림 조회 서비스</h2>
 * <p>알림 조회 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService implements NotificationQueryUseCase {

    private final LoadNotificationPort loadNotificationPort;

    @Override
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {
        return loadNotificationPort.getNotificationList(userDetails);
    }
}