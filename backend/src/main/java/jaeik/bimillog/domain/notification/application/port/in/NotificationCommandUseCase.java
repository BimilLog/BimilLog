package jaeik.bimillog.domain.notification.application.port.in;

import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.infrastructure.adapter.in.notification.web.NotificationCommandController;

/**
 * <h2>알림 명령 유스케이스</h2>
 * <p>알림 도메인의 명령 작업을 담당하는 유스케이스입니다.</p>
 * <p>알림 읽음 처리, 알림 삭제</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandUseCase {

    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>여러 알림에 대해 읽음 처리 또는 삭제를 일괄 수행합니다.</p>
     * <p>읽음 처리: isRead 상태를 true로 변경</p>
     * <p>삭제: 알림을 완전 제거</p>
     * <p>{@link NotificationCommandController}에서 사용자의 알림 관리 API 요청 시 호출됩니다.</p>
     *
     * @param userDetails   현재 로그인한 유저 정보
     * @param updateCommand 알림 업데이트 명령 객체 (읽음/삭제 여부와 대상 알림 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    void batchUpdate(Long userId, NotificationUpdateVO updateCommand);

    /**
     * <h3>특정 사용자의 알림 전체 삭제</h3>
     * <p>구현 필요</p>
     * @param userDetails 현재 로그인한 사용자 정보
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllNotification(Long userId);
}