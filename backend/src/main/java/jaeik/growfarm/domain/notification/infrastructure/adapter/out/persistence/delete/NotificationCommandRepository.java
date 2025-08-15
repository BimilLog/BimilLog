package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.delete;

import java.util.List;

/**
 * <h2>알림 명령 레포지터리 인터페이스</h2>
 * <p>

 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandRepository {

    /**
     * <h3>알림 삭제</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 삭제합니다.
     * </p>
     *
     * @param ids    삭제할 알림 ID 목록
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteByIdInAndUserId(List<Long> ids, Long userId);

    /**
     * <h3>알림 읽음 처리</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 읽음 처리합니다.
     * </p>
     *
     * @param ids    읽음 처리할 알림 ID 목록
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void markAsReadByIdInAndUserId(List<Long> ids, Long userId);

}