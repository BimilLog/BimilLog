package jaeik.growfarm.domain.notification.application.port.in;

import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;

import java.util.List;

/**
 * <h2>알림 조회 유스케이스</h2>
 * <p>알림 조회 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryUseCase {

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>현재 로그인한 유저의 알림 리스트를 조회합니다.</p>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return 알림 리스트
     */
    List<NotificationDTO> getNotificationList(CustomUserDetails userDetails);
}