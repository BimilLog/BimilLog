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
 * 이벤트 기반 아키텍처로 SSE와 FCM 알림을 분리 처리
 * </p>
 *
 * @author Jaeik
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final SseService sseService;
    private final FcmService fcmService;

    // ================== 댓글 작성 이벤트 처리 ==================

    /**
     * <h3>댓글 작성 SSE 알림 처리</h3>
     * 
     * @param event 댓글 작성 이벤트
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handleCommentCreatedEventForSse(CommentCreatedEvent event) {
        log.info("댓글 작성 SSE 이벤트 처리 시작: postUserId={}, 스레드={}",
                event.getPostUserId(), Thread.currentThread().getName());

        sseService.sendCommentNotificationAsync(
                event.getPostUserId(),
                event.getCommenterName(),
                event.getPostId());

        log.info("댓글 작성 SSE 이벤트 처리 완료: postUserId={}", event.getPostUserId());
    }

    /**
     * <h3>댓글 작성 FCM 알림 처리</h3>
     * 
     * @param event 댓글 작성 이벤트
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handleCommentCreatedEventForFcm(CommentCreatedEvent event) {
        log.info("댓글 작성 FCM 이벤트 처리 시작: postUserId={}, 스레드={}",
                event.getPostUserId(), Thread.currentThread().getName());

        fcmService.sendCommentFcmNotificationAsync(
                event.getPostOwner(),
                event.getCommenterName());

        log.info("댓글 작성 FCM 이벤트 처리 완료: postUserId={}", event.getPostUserId());
    }

    // ================== 농작물 심기 이벤트 처리 ==================

    /**
     * <h3>농작물 심기 SSE 알림 처리</h3>
     * 
     * @param event 농작물 심기 이벤트
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handlePaperPlantEventForSse(MessageEvent event) {
        log.info("농작물 심기 SSE 이벤트 처리 시작: farmOwnerId={}, 스레드={}",
                event.getPaperOwnerId(), Thread.currentThread().getName());

        sseService.sendPaperPlantNotificationAsync(
                event.getPaperOwnerId(),
                event.getUserName());

        log.info("농작물 심기 SSE 이벤트 처리 완료: farmOwnerId={}", event.getPaperOwnerId());
    }

    /**
     * <h3>농작물 심기 FCM 알림 처리</h3>
     * 
     * @param event 농작물 심기 이벤트
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handlePaperPlantEventForFcm(MessageEvent event) {
        log.info("농작물 심기 FCM 이벤트 처리 시작: farmOwnerId={}, 스레드={}",
                event.getPaperOwnerId(), Thread.currentThread().getName());

        fcmService.sendPaperPlantFcmNotificationAsync(event.getPaperOwner());

        log.info("농작물 심기 FCM 이벤트 처리 완료: farmOwnerId={}", event.getPaperOwnerId());
    }

    // ================== 인기글 등극 이벤트 처리 ==================

    /**
     * <h3>인기글 등극 SSE 알림 처리</h3>
     * 
     * @param event 인기글 등극 이벤트
     */
    @EventListener
    @Async("sseNotificationExecutor")
    public void handlePostFeaturedEventForSse(PostFeaturedEvent event) {
        log.info("인기글 등극 SSE 이벤트 처리 시작: userId={}, 스레드={}",
                event.getUserId(), Thread.currentThread().getName());

        sseService.sendPostFeaturedNotificationAsync(
                event.getUserId(),
                event.getSseMessage(),
                event.getPostId());

        log.info("인기글 등극 SSE 이벤트 처리 완료: userId={}", event.getUserId());
    }

    /**
     * <h3>인기글 등극 FCM 알림 처리</h3>
     * 
     * @param event 인기글 등극 이벤트
     */
    @EventListener
    @Async("fcmNotificationExecutor")
    public void handlePostFeaturedEventForFcm(PostFeaturedEvent event) {
        log.info("인기글 등극 FCM 이벤트 처리 시작: userId={}, 스레드={}",
                event.getUserId(), Thread.currentThread().getName());

        fcmService.sendPostFeaturedFcmNotificationAsync(
                event.getUser(),
                event.getFcmTitle(),
                event.getFcmBody());

        log.info("인기글 등극 FCM 이벤트 처리 완료: userId={}", event.getUserId());
    }
}