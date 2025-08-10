package jaeik.growfarm.service.notification;

import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.repository.notification.NotificationRepository;
import jaeik.growfarm.repository.notification.delete.NotificationDeleteRepository;
import jaeik.growfarm.repository.notification.update.NotificationUpdateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>알림 업데이트 서비스</h2>
 * <p>
 * 알림의 DB작업을 처리하는 서비스
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationUpdateService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeleteRepository notificationDeleteRepository;
    private final NotificationUpdateRepository notificationUpdateRepository;

    /**
     * <h3>알림 저장</h3>
     * <p>
     * 사용자에게 알림을 저장합니다.
     * </p>
     *
     * @param user 사용자 정보
     * @param type 알림 유형
     * @param data 알림 데이터
     * @param url  알림 URL
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void saveNotification(Users user, NotificationType type, String data, String url) {
        notificationRepository.save(Notification.createNotification(user, type, data, url));
    }

    /**
     * <h3>알림 삭제</h3>
     * <p>
     * 사용자의 알림을 삭제합니다.
     * </p>
     *
     * @param deleteIds 삭제할 알림 ID 목록
     * @param userId    사용자 ID
     * @throws CustomException 권한이 없는 알림이 포함된 경우
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void deleteNotifications(List<Long> deleteIds, Long userId) {
        if (deleteIds == null || deleteIds.isEmpty()) {
            return;
        }

        notificationDeleteRepository.deleteByIdInAndUserId(deleteIds, userId);
    }

    /**
     * <h3>알림 읽음 처리</h3>
     * <p>
     * 사용자의 알림을 읽음 상태로 변경합니다.
     * </p>
     *
     * @param readIds 읽음 처리할 알림 ID 목록
     * @param userId  사용자 ID
     * @throws CustomException 권한이 없는 알림이 포함된 경우
     * @author Jaeik
     * @since 1.0.0
     */
    @Transactional
    public void markNotificationsAsRead(List<Long> readIds, Long userId) {
        if (readIds == null || readIds.isEmpty()) {
            return;
        }

        notificationUpdateRepository.markAsReadByIdInAndUserId(readIds, userId);
    }
}
