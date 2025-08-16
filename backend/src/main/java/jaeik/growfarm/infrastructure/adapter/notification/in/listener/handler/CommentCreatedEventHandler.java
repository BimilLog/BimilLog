package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.growfarm.domain.notification.application.port.in.NotificationFcmUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * <h2>댓글 생성 이벤트 핸들러</h2>
 * <p>
 *     {@link CommentCreatedEvent}를 처리하여 댓글 생성 알림을 전송합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentCreatedEventHandler implements NotificationEventHandler<CommentCreatedEvent> {

    private final NotificationSseUseCase notificationSseUseCase;
    private final NotificationFcmUseCase notificationFcmUseCase;

    /**
     * <h3>이벤트 지원 여부 확인</h3>
     * <p>주어진 이벤트가 {@link CommentCreatedEvent} 타입인지 확인합니다.</p>
     *
     * @param event 확인할 {@link ApplicationEvent}
     * @return 이벤트가 {@link CommentCreatedEvent} 타입이면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean supports(ApplicationEvent event) {
        return event instanceof CommentCreatedEvent;
    }

    /**
     * <h3>이벤트 처리</h3>
     * <p>{@link CommentCreatedEvent}를 처리하여 댓글 생성 알림을 전송합니다.</p>
     *
     * @param event 처리할 {@link CommentCreatedEvent}
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void handle(CommentCreatedEvent event) {
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
}
