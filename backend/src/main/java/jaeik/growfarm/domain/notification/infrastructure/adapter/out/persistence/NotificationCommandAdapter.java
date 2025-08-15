package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import jaeik.growfarm.domain.notification.application.port.out.NotificationCommandPort;
import jaeik.growfarm.domain.notification.entity.Notification;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.delete.NotificationCommandRepository;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.read.NotificationReadRepository;
import jaeik.growfarm.domain.user.entity.User;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
public class NotificationCommandAdapter implements NotificationCommandPort {

    private final NotificationRepository notificationRepository;
    private final NotificationCommandRepository notificationCommandRepository;



    /**
     * <h3>알림 일괄 업데이트</h3>
     * <p>주어진 알림 ID 목록에 따라 알림을 삭제하거나 읽음 상태로 변경합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param updateNotificationDTO 업데이트할 알림 정보 DTO (삭제할 ID 목록, 읽음 처리할 ID 목록 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void batchUpdate(CustomUserDetails userDetails, UpdateNotificationDTO updateNotificationDTO) {
        Long userId = userDetails.getUserId();
        List<Long> deleteIds = updateNotificationDTO.getDeletedIds();
        List<Long> readIds = updateNotificationDTO.getReadIds();

        if (deleteIds != null && !deleteIds.isEmpty()) {
            notificationCommandRepository.deleteByIdInAndUserId(deleteIds, userId);
        }

        if (readIds != null && !readIds.isEmpty()) {
            notificationCommandRepository.markAsReadByIdInAndUserId(readIds, userId);
        }
    }

    /**
     * <h3>알림 저장</h3>
     * <p>새로운 알림을 데이터베이스에 저장합니다.</p>
     *
     * @param user 알림을 받을 사용자 엔티티
     * @param type 알림 유형
     * @param content 알림 내용
     * @param url 알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void save(User user, NotificationType type, String content, String url) {
        notificationRepository.save(Notification.create(user, type, content, url));
    }
}