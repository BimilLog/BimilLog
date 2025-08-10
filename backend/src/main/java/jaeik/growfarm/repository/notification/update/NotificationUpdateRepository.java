package jaeik.growfarm.repository.notification.update;

import java.util.List;

/**
 * <h2>알림 업데이트 레포지터리 인터페이스</h2>
 * <p>
 * 알림 상태 변경 관련 기능만 담당하는 인터페이스
 * SRP: 알림 상태 변경만 담당
 * ISP: 업데이트 기능만 노출
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public interface NotificationUpdateRepository {

    /**
     * <h3>알림 읽음 처리</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 읽음 처리합니다.
     * </p>
     *
     * @param ids    읽음 처리할 알림 ID 목록
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 1.0.0
     */
    void markAsReadByIdInAndUserId(List<Long> ids, Long userId);

}