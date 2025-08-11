package jaeik.growfarm.domain.notification.application.port.in;

import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

/**
 * <h2>알림 명령 유스케이스</h2>
 * <p>알림 상태 변경 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandUseCase {

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>현재 로그인한 유저의 알림을 읽음 처리하거나 삭제합니다.</p>
     *
     * @param userDetails           현재 로그인한 유저 정보
     * @param updateNotificationDTO 알림 업데이트 정보
     */
    void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO);
}