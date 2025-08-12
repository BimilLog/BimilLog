package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.growfarm.domain.notification.application.port.out.UpdateNotificationPort;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <h2>알림 명령 서비스</h2>
 * <p>알림 상태 변경 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationCommandService implements NotificationCommandUseCase {

    private final UpdateNotificationPort updateNotificationPort;

    @Override
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        updateNotificationPort.batchUpdate(userDetails, updateNotificationDTO);
    }
}