package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.LoadNotificationPort;
import jaeik.growfarm.domain.notification.application.port.out.SaveNotificationPort;
import jaeik.growfarm.domain.notification.application.port.out.UpdateNotificationPort;
import jaeik.growfarm.domain.notification.domain.Notification;
import jaeik.growfarm.domain.notification.domain.NotificationType;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.NotificationRepository;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.delete.NotificationDeleteRepository;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.read.NotificationReadRepository;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.update.NotificationUpdateRepository;
import jaeik.growfarm.domain.user.domain.User;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.global.exception.CustomException;
import jaeik.growfarm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <h2>알림 JPA 어댑터</h2>
 * <p>알림 데이터 조회 및 업데이트를 위한 JPA 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class NotificationJpaAdapter implements LoadNotificationPort, UpdateNotificationPort, SaveNotificationPort {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final NotificationDeleteRepository notificationDeleteRepository;
    private final NotificationUpdateRepository notificationUpdateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationList(CustomUserDetails userDetails) {
        return notificationReadRepository.findNotificationsByUserIdOrderByLatest(userDetails.getUserId());
    }

    @Override
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        Long userId = userDetails.getUserId();
        List<Long> deleteIds = updateNotificationDTO.getDeletedIds();
        List<Long> readIds = updateNotificationDTO.getReadIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationDeleteRepository.deleteByIdInAndUserId(deleteIds, userId);
        }

        if (readIds != null && !readIds.isEmpty()) {
            notificationUpdateRepository.markAsReadByIdInAndUserId(readIds, userId);
        }
    }

    @Override
    public void save(User user, NotificationType type, String content, String url) {
        notificationRepository.save(Notification.create(user, type, content, url));
    }
}