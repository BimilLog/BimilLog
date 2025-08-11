package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSenderPort;
import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <h2>알림 이벤트 서비스</h2>
 * <p>다양한 이벤트 기반 알림 전송 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationEventService implements NotificationEventUseCase {

    private final NotificationSenderPort notificationSenderPort;
    private final NotificationUtilPort notificationUtilPort;

    @Value("${url}")
    private String url;

    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.COMMENT,
                commenterName + "님이 댓글을 남겼습니다!",
                url + "/board/post/" + postId
        );

        notificationSenderPort.sendNotificationToAll(postUserId, eventDTO);
    }

    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.FARM,
                "롤링페이퍼에 메시지가 작성되었어요!",
                url + "/rolling-paper/" + userName
        );

        notificationSenderPort.sendNotificationToAll(farmOwnerId, eventDTO);
    }

    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.POST_FEATURED,
                message,
                url + "/board/post/" + postId
        );

        notificationSenderPort.sendNotificationToAll(userId, eventDTO);
    }
}