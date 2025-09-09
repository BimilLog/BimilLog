package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.comment.event.CommentCreatedEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationFcmUseCase;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.domain.paper.event.RollingPaperEvent;
import jaeik.bimillog.domain.post.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 생성 이벤트 리스너</h2>
 * <p>
 * 헥사고날 아키텍처의 Primary Adapter로서 다양한 도메인에서 발행하는 비즈니스 이벤트를 수신합니다.
 * </p>
 * <p>
 * 사용자가 댓글을 작성하거나 롤링페이퍼에 메시지를 남기는 상황에서 해당 도메인의 CommandService가 
 * 이벤트를 발행하면, 이를 수신하여 관련된 사용자에게 실시간 알림을 전송하는 역할을 담당합니다.
 * </p>
 * <p>회원탈퇴, 댓글 작성, 롤링페이퍼 작성, 인기글 선정 등의 비즈니스 이벤트 발생 시 NotificationSseUseCase와 
 * NotificationFcmUseCase를 통해 브라우저 실시간 알림과 모바일 푸시 알림을 병렬로 전송합니다.</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationGenerateListener {

    private final NotificationSseUseCase notificationSseUseCase;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>댓글 작성 완료 시 게시글 작성자 알림 전송</h3>
     * <p>사용자가 게시글에 댓글을 작성하는 상황에서 CommentCommandService가 댓글 저장을 완료한 후 
     * CommentCreatedEvent를 발행하면, 이를 수신하여 게시글 작성자에게 새 댓글 알림을 전송합니다.</p>
     * <p>게시글 작성자가 브라우저에 접속 중이면 SSE로 실시간 알림을, 모바일 앱 사용자라면 FCM으로 푸시 알림을 
     * 병렬 전송하여 댓글 작성 즉시 알림을 받을 수 있도록 보장합니다.</p>
     * <p>알림 설정에서 댓글 알림을 비활성화한 사용자나 댓글 작성자 본인에게는 알림을 전송하지 않습니다.</p>
     * 
     * @param event 댓글 생성 완료 이벤트 (게시글 ID, 게시글 작성자 ID, 댓글 작성자명 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(CommentCreatedEvent.class)
    @Async("sseNotificationExecutor")
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendCommentNotification(
                event.postUserId(),
                event.commenterName(),
                event.postId());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendCommentNotification(
                event.postUserId(),
                event.commenterName());
    }

    /**
     * <h3>롤링페이퍼 메시지 작성 완료 시 종이 주인 알림 전송</h3>
     * <p>사용자가 다른 사용자의 롤링페이퍼에 메시지를 작성하는 상황에서 PaperCommandService가 메시지 저장을 완료한 후 
     * RollingPaperEvent를 발행하면, 이를 수신하여 롤링페이퍼 주인에게 새 메시지 도착 알림을 전송합니다.</p>
     * <p>롤링페이퍼 주인이 브라우저에 접속 중이면 SSE로 실시간 알림을, 모바일에서는 FCM으로 푸시 알림을 
     * 병렬 전송하여 메시지 작성 즉시 확인할 수 있도록 합니다.</p>
     * <p>롤링페이퍼 주인이 알림을 비활성화했거나 메시지 작성자 본인인 경우에는 알림을 전송하지 않습니다.</p>
     * 
     * @param event 롤링페이퍼 메시지 생성 완료 이벤트 (롤링페이퍼 주인 ID, 메시지 작성자명 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(RollingPaperEvent.class)
    @Async("sseNotificationExecutor")
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendPaperPlantNotification(
                event.paperOwnerId(),
                event.userName());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendPaperPlantNotification(
                event.paperOwnerId());
    }

    /**
     * <h3>게시글 인기글 선정 시 작성자 알림 전송</h3>
     * <p>관리자가 게시글을 인기글로 선정하거나 시스템 스케줄러가 인기글을 자동 선정하는 상황에서 
     * PostCommandService가 인기글 지정을 완료한 후 PostFeaturedEvent를 발행하면, 
     * 이를 수신하여 게시글 작성자에게 인기글 선정 축하 알림을 전송합니다.</p>
     * <p>게시글 작성자가 브라우저에 접속 중이면 SSE로 실시간 축하 메시지를, 모바일에서는 FCM으로 푸시 알림을 
     * 병렬 전송하여 인기글 선정 소식을 즉시 확인할 수 있도록 합니다.</p>
     * <p>인기글 알림을 비활성화한 사용자에게는 알림을 전송하지 않습니다.</p>
     * 
     * @param event 인기글 선정 완료 이벤트 (게시글 작성자 ID, 게시글 ID, SSE/FCM 메시지 내용 포함)
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(PostFeaturedEvent.class)
    @Async("sseNotificationExecutor")
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        // SSE 알림 전송
        notificationSseUseCase.sendPostFeaturedNotification(
                event.userId(),
                event.sseMessage(),
                event.postId());
        
        // FCM 알림 전송
        notificationFcmUseCase.sendPostFeaturedNotification(
                event.userId(),
                event.fcmTitle(),
                event.fcmBody());
    }
}
