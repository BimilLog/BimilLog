package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.in.web.NotificationQueryController;
import jaeik.bimillog.domain.auth.out.CustomUserDetails;

import java.util.List;

/**
 * <h2>알림 조회 유스케이스</h2>
 * <p>알림 도메인의 조회 작업을 담당하는 유스케이스입니다.</p>
 * <p>알림 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryUseCase {

    /**
     * <h3>알림 목록 조회</h3>
     * <p>로그인한 사용자의 모든 알림을 최신순으로 조회합니다.</p>
     * <p>읽음/읽지 않음 상태, 알림 유형, 생성 시간 정보를 포함합니다.</p>
     * <p>{@link NotificationQueryController}에서 사용자의 알림함 조회 API 요청 시 호출됩니다.</p>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return 알림 리스트 (최신순 정렬, 읽음 상태 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(CustomUserDetails userDetails);
}