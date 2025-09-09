package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.Notification;

import java.util.List;

/**
 * <h2>알림 조회 포트</h2>
 * <p>
 * 헥사고날 아키텍처에서 알림 도메인의 조회형 데이터 저장소 연동을 정의하는 Secondary Port입니다.
 * 사용자별 알림 목록 조회에 대한 외부 어댑터 인터페이스를 제공합니다.
 * </p>
 * <p>
 * CQRS 패턴을 적용하여 읽기 전용 작업에 최적화된 쿼리를 수행하며,
 * 알림 목록 조회 성능을 향상시키기 위한 인덱싱과 정렬을 지원합니다.
 * </p>
 * <p>NotificationQueryService에서 사용되며, NotificationQueryAdapter에 의해 구현합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryPort {

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>특정 사용자가 받은 모든 알림을 최신순으로 조회하여 반환합니다.</p>
     * <p>읽음/읽지 않음 상태, 알림 유형, 생성 시간, 관련 URL 등의 정보를 포함하여 조회합니다.</p>
     * <p>사용자 ID 기준으로 인덱싱되어 있어 빠른 조회 성능을 제공하며, 생성일 내림차순으로 정렬됩니다.</p>
     * <p>NotificationQueryService에서 사용자의 알림함 조회 시 호출됩니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 알림 리스트 (최신순 정렬, 읽음 상태 및 메타데이터 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(Long userId);
}