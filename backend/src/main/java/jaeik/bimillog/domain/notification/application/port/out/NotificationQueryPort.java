package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.Notification;

import java.util.List;

/**
 * <h2>알림 조회 포트</h2>
 * <p>알림 조회 관련 데이터 액세스를 정의하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryPort {

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>특정 사용자의 알림 목록을 조회합니다.</p>
     *
     * @param userId 사용자 ID
     * @return 알림 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(Long userId);
}