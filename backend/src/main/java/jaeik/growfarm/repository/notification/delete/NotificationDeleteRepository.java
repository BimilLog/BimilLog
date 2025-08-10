package jaeik.growfarm.repository.notification.delete;

import java.util.List;

/**
 * <h2>알림 삭제 레포지터리 인터페이스</h2>
 * <p>
 * 알림 삭제 관련 기능만 담당하는 인터페이스
 * SRP: 알림 삭제만 담당
 * ISP: 삭제 기능만 노출
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
public interface NotificationDeleteRepository {

    /**
     * <h3>알림 삭제</h3>
     * <p>
     * 알림 ID 목록과 사용자 ID를 기준으로 알림을 삭제합니다.
     * </p>
     *
     * @param ids    삭제할 알림 ID 목록
     * @param userId 사용자 ID
     * @author Jaeik
     * @since 1.0.0
     */
    void deleteByIdInAndUserId(List<Long> ids, Long userId);

}