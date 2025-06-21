package jaeik.growfarm.global.listener;

import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.global.event.MessageEvent;
import jaeik.growfarm.global.event.PostFeaturedEvent;
import jaeik.growfarm.service.notification.FcmService;
import jaeik.growfarm.service.notification.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 이벤트 리스너</h2>
 * <p>
 * 모든 알림 관련 이벤트를 처리하는 리스너
 * </p>
 * <p>
 * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 분리 처리
 * </p>
 * @author Jaeik
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final SseService sseService;
    private final FcmService fcmService;

    /**
     * <h3>댓글 달림 SSE 알림 처리</h3>
     * 
     * @param event 댓글 작성 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handleCommentCreatedEventForSse(CommentCreatedEvent event) {
        sseService.sendCommentNotificationAsync(
                event.getPostUserId(),
                event.getCommenterName(),
                event.getPostId());
    }

    /**
     * <h3>댓글 달림 FCM 알림 처리</h3>
     * 
     * @param event 댓글 작성 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handleCommentCreatedEventForFcm(CommentCreatedEvent event) {
        fcmService.sendCommentFcmNotificationAsync(
                event.getPostOwner(),
                event.getCommenterName());
    }

    /**
     * <h3>롤링페이퍼에 메시지 수신 SSE 알림 처리</h3>
     * 
     * @param event 농작물 심기 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handlePaperPlantEventForSse(MessageEvent event) {
        sseService.sendPaperPlantNotificationAsync(
                event.getPaperOwnerId(),
                event.getUserName());
    }

    /**
     * <h3>롤링페이퍼에 메시지 수신 FCM 알림 처리</h3>
     * 
     * @param event 농작물 심기 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handlePaperPlantEventForFcm(MessageEvent event) {
        fcmService.sendPaperPlantFcmNotificationAsync(event.getPaperOwner());
    }

    /**
     * <h3>인기글 등극 SSE 알림 처리</h3>
     * 
     * @param event 인기글 등극 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handlePostFeaturedEventForSse(PostFeaturedEvent event) {
        sseService.sendPostFeaturedNotificationAsync(
                event.getUserId(),
                event.getSseMessage(),
                event.getPostId());
    }

    /**
     * <h3>인기글 등극 FCM 알림 처리</h3>
     * 
     * @param event 인기글 등극 이벤트
     * @author Jaeik
     * @since 1.0.0
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handlePostFeaturedEventForFcm(PostFeaturedEvent event) {
        fcmService.sendPostFeaturedFcmNotificationAsync(
                event.getUser(),
                event.getFcmTitle(),
                event.getFcmBody());
    }
}