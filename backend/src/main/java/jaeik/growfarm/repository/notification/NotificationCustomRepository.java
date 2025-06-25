package jaeik.growfarm.repository.notification;

import jaeik.growfarm.dto.notification.NotificationDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 커스텀 저장소 인터페이스</h2>
 * <p>
 * 알림 관련 데이터베이스 작업을 수행하는 커스텀 저장소 인터페이스입니다.
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Repository
public interface NotificationCustomRepository {

    /**
     * <h3>사용자 ID로 알림 목록 조회</h3>
     * <p>
     * 사용자 ID를 기준으로 알림 목록을 최신순으로 조회합니다.
     * </p>
     *
     * @param userId 사용자 ID
     * @return List<NotificationDTO> 알림 DTO 목록
     * @author Jaeik
     * @since 1.0.0
     */
    List<NotificationDTO> findNotificationsByUserIdOrderByLatest(Long userId);

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
