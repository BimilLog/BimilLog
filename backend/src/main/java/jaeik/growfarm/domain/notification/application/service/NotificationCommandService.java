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

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>사용자의 알림 상태를 일괄적으로 업데이트합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param updateNotificationDTO 업데이트할 알림 정보 DTO
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        updateNotificationPort.batchUpdate(userDetails, updateNotificationDTO);
    }
}