package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.NotificationSenderPort;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>알림 전송 어댑터</h2>
 * <p>다양한 알림 전송 방식을 통합 관리하는 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class NotificationSenderAdapter implements NotificationSenderPort {

    private final List<NotificationSender> notificationSenders;

    @Override
    public void sendNotificationToAll(Long userId, EventDTO eventDTO) {
        for (NotificationSender sender : notificationSenders) {
            try {
                sender.send(userId, eventDTO);
                log.debug("알림 전송 성공 - Type: {}, UserId: {}", sender.getType(), userId);
            } catch (Exception e) {
                log.error("알림 전송 실패 - Type: {}, UserId: {}, Error: {}", 
                         sender.getType(), userId, e.getMessage(), e);
            }
        }
    }
}