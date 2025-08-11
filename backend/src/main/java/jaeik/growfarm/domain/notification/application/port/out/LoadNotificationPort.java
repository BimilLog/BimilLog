package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;

import java.util.List;

/**
 * <h2>알림 조회 포트</h2>
 * <p>알림 조회 관련 데이터 액세스를 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface LoadNotificationPort {

    /**
     * <h3>알림 리스트 조회</h3>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return 알림 리스트
     */
    List<NotificationDTO> getNotificationList(CustomUserDetails userDetails);
}