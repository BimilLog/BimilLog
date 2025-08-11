package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.domain.notification.application.port.out.NotificationSender;
import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.notification.domain.NotificationType;
import jaeik.growfarm.dto.notification.EventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventService implements NotificationEventUseCase {

    private final List<NotificationSender> notificationSenders;
    private final NotificationUtilPort notificationUtilPort;

    @Value("${url}")
    private String url;

    private void sendNotificationToAll(Long userId, EventDTO eventDTO) {
        if (notificationUtilPort.isEligibleForNotification(userId, eventDTO.getType())) {
            for (NotificationSender sender : notificationSenders) {
                try {
                    sender.send(userId, eventDTO);
                    log.debug("알림 전송 성공 - Sender: {}, UserId: {}", sender.getClass().getSimpleName(), userId);
                } catch (Exception e) {
                    log.error("알림 전송 실패 - Sender: {}, UserId: {}, Error: {}",
                            sender.getClass().getSimpleName(), userId, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.COMMENT,
                commenterName + "님이 댓글을 남겼습니다!",
                url + "/board/post/" + postId
        );

        sendNotificationToAll(postUserId, eventDTO);
    }

    @Override
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.MESSAGE,
                "롤링페이퍼에 메시지가 작성되었어요!",
                url + "/rolling-paper/" + userName
        );

        sendNotificationToAll(farmOwnerId, eventDTO);
    }

    @Override
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        EventDTO eventDTO = notificationUtilPort.createEventDTO(
                NotificationType.POST_FEATURED,
                message,
                url + "/board/post/" + postId
        );

        sendNotificationToAll(userId, eventDTO);
    }
}