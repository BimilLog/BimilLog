package jaeik.growfarm.domain.auth.infrastructure.adapter.out.persistence.notification;

import jaeik.growfarm.domain.auth.application.port.out.ManageNotificationPort;
import jaeik.growfarm.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 어댑터</h2>
 * <p>Auth 도메인에서 Notification 도메인의 In-Port를 통해 접근하는 어댑터</p>
 * <p>헥사고날 아키텍처를 준수하여 UseCase를 통한 도메인간 통신</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements ManageNotificationPort {

    private final NotificationCommandUseCase notificationCommandUseCase;
    private final NotificationSseUseCase notificationSSEUseCase;

    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        notificationSSEUseCase.deleteAllEmitterByUserId(userId);
    }
}