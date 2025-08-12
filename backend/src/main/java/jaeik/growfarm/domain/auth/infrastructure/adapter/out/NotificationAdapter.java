package jaeik.growfarm.domain.auth.infrastructure.adapter.out;

import jaeik.growfarm.domain.auth.application.port.out.ManageNotificationPort;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 어댑터</h2>
 * <p>SSE 연결 관리를 위한 어댑터</p>
 * <p>책임 분리 원칙에 따라 알림 관련 작업만을 담당</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements ManageNotificationPort {

    private final EmitterRepository emitterRepository;

    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        emitterRepository.deleteAllEmitterByUserId(userId);
    }
}