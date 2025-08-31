package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.NotificationInfo;

import java.util.List;

/**
 * <h2>알림 조회 포트</h2>
 * <p>알림 조회 관련 데이터 액세스를 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryPort {

    /**
     * <h3>알림 리스트 조회</h3>
     *
     * @param userId 사용자 ID
     * @return 알림 리스트
     */
    List<NotificationInfo> getNotificationList(Long userId);
}