package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;

import java.util.List;

/**
 * <h2>알림 조회 사용 사례</h2>
 * <p>알림 조회 관련 비즈니스 로직을 정의하는 인바운드 포트</p>
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
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(CustomUserDetails userDetails);
}