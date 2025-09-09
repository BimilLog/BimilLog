package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;

import java.util.List;

/**
 * <h2>알림 조회 요구사항</h2>
 * <p>
 * 헥사고날 아키텍처에서 알림 도메인의 조회형 작업을 정의하는 Primary Port입니다.
 * 사용자의 알림 목록 조회와 관련된 모든 읽기 작업의 비즈니스 인터페이스를 제공합니다.
 * </p>
 * <p>
 * CQRS 패턴을 적용하여 명령과 조회를 명확히 분리하며, 알림 조회 성능을 최적화합니다.
 * 사용자별 알림 목록을 효율적으로 조회하고, 읽음/읽지 않음 상태 정보를 함께 제공합니다.
 * </p>
 * <p>NotificationQueryController에서 알림 목록 조회 API 요청을 처리하기 위해 호출합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationQueryUseCase {

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>현재 로그인한 사용자의 모든 알림을 최신순으로 조회합니다.</p>
     * <p>읽음/읽지 않음 상태, 알림 유형, 생성 시간 등의 정보를 포함하여 반환합니다.</p>
     * <p>페이징 처리 없이 모든 알림을 조회하며, 프론트엔드에서 필터링과 정렬을 수행할 수 있도록 지원합니다.</p>
     * <p>NotificationQueryController에서 사용자의 알림함 조회 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return 알림 리스트 (최신순 정렬, 읽음 상태 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    List<Notification> getNotificationList(CustomUserDetails userDetails);
}