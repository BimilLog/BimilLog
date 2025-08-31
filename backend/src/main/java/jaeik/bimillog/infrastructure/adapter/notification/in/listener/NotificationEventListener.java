package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 이벤트 리스너</h2>
 * <p>도메인 이벤트를 수신하여 SSE와 FCM 알림을 전송하는 리스너입니다.</p>
 * <p>각 이벤트 타입별로 적절한 알림을 처리합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationSseUseCase notificationSseUseCase;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>댓글 생성 알림 이벤트 처리</h3>
     * <p>댓글 생성 이벤트를 처리하여 관련 알림을 전송합니다.</p>
     * @param event 댓글 생성 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(CommentCreatedEvent.class)
    @Async("sseNotificationExecutor")
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendCommentNotification(
                event.getPostUserId(),
                event.getCommenterName(),
                event.getPostId());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendCommentNotification(
                event.getPostUserId(),
                event.getCommenterName());
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 이벤트 처리</h3>
     * <p>롤링페이퍼 메시지 수신 이벤트를 처리하여 관련 알림을 전송합니다.</p>
     * @param event 롤링페이퍼 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(RollingPaperEvent.class)
    @Async("sseNotificationExecutor")
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendPaperPlantNotification(
                event.getPaperOwnerId(),
                event.getUserName());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendPaperPlantNotification(
                event.getPaperOwnerId());
    }

    /**
     * <h3>인기글 선정 알림 이벤트 처리</h3>
     * <p>인기글 선정 이벤트를 처리하여 관련 알림을 전송합니다.</p>
     * @param event 인기글 선정 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(PostFeaturedEvent.class)
    @Async("sseNotificationExecutor")
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendPostFeaturedNotification(
                event.getUserId(),
                event.getSseMessage(),
                event.getPostId());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendPostFeaturedNotification(
                event.getUserId(),
                event.getFcmTitle(),
                event.getFcmBody());
    }
}
