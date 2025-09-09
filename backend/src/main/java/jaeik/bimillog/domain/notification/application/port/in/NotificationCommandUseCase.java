package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;

/**
 * <h2>알림 명령 요구사항</h2>
 * <p>
 * 헥사고날 아키텍처에서 알림 도메인의 명령형 작업을 정의하는 Primary Port입니다.
 * 알림의 상태 변경 및 삭제와 관련된 모든 쓰기 작업의 비즈니스 인터페이스를 제공합니다.
 * </p>
 * <p>
 * 사용자가 알림을 읽음 처리하거나 삭제하는 모든 명령형 요청을 처리합니다.
 * CQRS 패턴을 적용하여 조회와 명령을 명확히 분리하며, 알림 상태 변경의 일관성을 보장합니다.
 * </p>
 * <p>NotificationCommandController에서 API 요청을 처리하기 위해 호출합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandUseCase {

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>사용자의 여러 알림에 대해 읽음 처리 또는 삭제를 일괄적으로 수행합니다.</p>
     * <p>읽음 처리 시 알림의 isRead 상태를 true로 변경하고, 삭제 시 알림을 물리적으로 제거합니다.</p>
     * <p>트랜잭션 내에서 처리되어 일관성을 보장하며, 다중 알림 처리의 성능을 최적화합니다.</p>
     * <p>NotificationCommandController에서 사용자의 알림 관리 API 요청을 처리하기 위해 호출합니다.</p>
     *
     * @param userDetails   현재 로그인한 유저 정보
     * @param updateCommand 알림 업데이트 명령 객체 (읽음/삭제 여부와 대상 알림 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void batchUpdate(CustomUserDetails userDetails, NotificationUpdateVO updateCommand);
}