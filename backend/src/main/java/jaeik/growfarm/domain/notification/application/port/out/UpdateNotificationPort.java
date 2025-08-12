package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

/**
 * <h2>알림 업데이트 포트</h2>
 * <p>알림 상태 변경 관련 데이터 액세스를 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface UpdateNotificationPort {

    /**
     * <h3>알림 일괄 업데이트</h3>
     *
     * @param userDetails           현재 로그인한 유저 정보
     * @param updateNotificationDTO 알림 업데이트 정보
     */
    void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO);
}