package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 이벤트 어댑터</h2>
 * <p>외부 이벤트로부터 알림 전송을 처리하는 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationEventAdapter {

    private final NotificationEventUseCase notificationEventUseCase;

    /**
     * <h3>댓글 알림 전송</h3>
     */
    public void sendCommentNotification(Long postUserId, String commenterName, Long postId) {
        notificationEventUseCase.sendCommentNotification(postUserId, commenterName, postId);
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 전송</h3>
     */
    public void sendPaperPlantNotification(Long farmOwnerId, String userName) {
        notificationEventUseCase.sendPaperPlantNotification(farmOwnerId, userName);
    }

    /**
     * <h3>인기글 등극 알림 전송</h3>
     */
    public void sendPostFeaturedNotification(Long userId, String message, Long postId) {
        notificationEventUseCase.sendPostFeaturedNotification(userId, message, postId);
    }
}