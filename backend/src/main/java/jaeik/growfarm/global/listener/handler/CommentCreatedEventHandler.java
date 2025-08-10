package jaeik.growfarm.global.listener.handler;

import jaeik.growfarm.global.event.CommentCreatedEvent;
import jaeik.growfarm.service.notification.NotificationFacadeService;
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
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class CommentCreatedEventHandler implements NotificationEventHandler<CommentCreatedEvent> {

    private final NotificationFacadeService notificationFacadeService;

    @Override
    public boolean supports(ApplicationEvent event) {
        return event instanceof CommentCreatedEvent;
    }

    @Override
    public void handle(CommentCreatedEvent event) {
        notificationFacadeService.sendCommentNotification(
                event.getPostUserId(),
                event.getCommenterName(),
                event.getPostId());
    }
}
