package jaeik.bimillog.domain.notification.in.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.FcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 생성 이벤트 리스너</h2>
 * <p>다양한 도메인에서 발행하는 비즈니스 이벤트를 수신합니다.</p>
 * <p>댓글 작성, 롤링페이퍼 작성, 인기글 선정 이벤트 처리</p>
 * <p>SSE 실시간 알림과 FCM 푸시 알림을 병렬 전송</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationGenerateListener {

    private final SseUseCase sseUseCase;
    private final FcmUseCase fcmUseCase;

    /**
     * <h3>댓글 작성 알림 전송</h3>
     * <p>댓글 작성 완료 시 게시글 작성자에게 SSE와 FCM 알림을 전송합니다.</p>
     * <p>{@link CommentCreatedEvent} 이벤트 발생시 댓글 작성 알림 전송 흐름에서 호출됩니다.</p>
     * 
     * @param event 댓글 생성 완료 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(CommentCreatedEvent.class)
    @Async("sseNotificationExecutor")
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        // SSE 알림 전송
        sseUseCase.sendCommentNotification(
                event.postUserId(),
                event.commenterName(),
                event.postId());
        
        // FCM 알림 전송
        fcmUseCase.sendCommentNotification(
                event.postUserId(),
                event.commenterName());
    }

    /**
     * <h3>롤링페이퍼 메시지 알림 전송</h3>
     * <p>롤링페이퍼 메시지 작성 완료 시 소유자에게 SSE와 FCM 알림을 전송합니다.</p>
     * <p>{@link RollingPaperEvent} 이벤트 발생시 롤링페이퍼 메시지 작성 알림 전송 흐름에서 호출됩니다.</p>
     * 
     * @param event 롤링페이퍼 메시지 생성 완료 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(RollingPaperEvent.class)
    @Async("sseNotificationExecutor")
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        // SSE 알림 전송
        sseUseCase.sendPaperPlantNotification(
                event.paperOwnerId(),
                event.memberName());
        
        // FCM 알림 전송
        fcmUseCase.sendPaperPlantNotification(
                event.paperOwnerId());
    }

    /**
     * <h3>인기글 등극 알림 전송</h3>
     * <p>게시글 인기글 선정 시 작성자에게 SSE와 FCM 알림을 전송합니다.</p>
     * <p>{@link PostFeaturedEvent} 이벤트 발생시 인기글 등극 알림 전송 흐름에서 호출됩니다.</p>
     * 
     * @param event 인기글 선정 완료 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(PostFeaturedEvent.class)
    @Async("sseNotificationExecutor")
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        // SSE 알림 전송
        sseUseCase.sendPostFeaturedNotification(
                event.memberId(),
                event.sseMessage(),
                event.postId());
        
        // FCM 알림 전송
        fcmUseCase.sendPostFeaturedNotification(
                event.memberId(),
                event.fcmTitle(),
                event.fcmBody());
    }
}
