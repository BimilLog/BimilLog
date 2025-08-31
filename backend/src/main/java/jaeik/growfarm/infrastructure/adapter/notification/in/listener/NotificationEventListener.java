package jaeik.growfarm.infrastructure.adapter.notification.in.listener;

import jaeik.growfarm.domain.comment.event.CommentCreatedEvent;
import jaeik.growfarm.domain.paper.event.RollingPaperEvent;
import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
import jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler.NotificationEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <h2>알림 이벤트 리스너 (디스패처)</h2>
 * <p>
 *     모든 {@link ApplicationEvent}를 수신하여, 해당 이벤트를 처리할 수 있는
 *     적절한 {@link NotificationEventHandler}에게 처리를 위임하는 디스패처 역할을 합니다.
 * </p>
 * <p>
 *     새로운 알림 유형이 추가되더라도 이 클래스는 수정할 필요가 없습니다.
 *     {@link NotificationEventHandler}를 구현한 핸들러만 추가하면 됩니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final List<NotificationEventHandler> notificationEventHandlers;

    /**
     * <h3>댓글 생성 알림 이벤트 처리</h3>
     * <p>댓글 생성 이벤트를 처리하여 관련 알림을 전송합니다.</p>
     * @param event 댓글 생성 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener(CommentCreatedEvent.class)
    @Async("sseNotificationExecutor")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        for (NotificationEventHandler handler : notificationEventHandlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                return;
            }
        }
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleRollingPaperEvent(RollingPaperEvent event) {
        for (NotificationEventHandler handler : notificationEventHandlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                return;
            }
        }
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handlePostFeaturedEvent(PostFeaturedEvent event) {
        for (NotificationEventHandler handler : notificationEventHandlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                return;
            }
        }
    }
}
