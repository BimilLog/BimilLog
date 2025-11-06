package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.service.NotificationQueryService;
import jaeik.bimillog.domain.notification.entity.Notification;

import java.util.List;

/**
 * <h2>알림 조회 포트</h2>
 * <p>알림 도메인의 조회 작업을 담당하는 포트입니다.</p>
 * <p>알림 목록 조회</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryPort {

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>특정 사용자가 받은 모든 알림을 최신순으로 조회하여 반환합니다.</p>
     * <p>읽음/읽지 않음 상태, 알림 유형, 생성 시간, 관련 URL 등의 정보를 포함하여 조회합니다.</p>
     * <p>생성일 내림차순으로 정렬됩니다.</p>
     * <p>{@link NotificationQueryService}에서 사용자의 알림함 조회 시 호출됩니다.</p>
     *
     * @param memberId 조회할 사용자 ID
     * @return 알림 리스트 (최신순 정렬, 읽음 상태 및 메타데이터 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(Long memberId);
}