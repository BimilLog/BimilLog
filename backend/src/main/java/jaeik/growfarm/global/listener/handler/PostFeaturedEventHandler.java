package jaeik.growfarm.global.listener.handler;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.global.event.PostFeaturedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * <h2>인기 게시글 선정 이벤트 핸들러</h2>
 * <p>
 *     {@link PostFeaturedEvent}를 처리하여 인기 게시글 선정 알림을 전송합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class PostFeaturedEventHandler implements NotificationEventHandler<PostFeaturedEvent> {

    private final NotificationEventUseCase notificationEventUseCase;

    @Override
    public boolean supports(ApplicationEvent event) {
        return event instanceof PostFeaturedEvent;
    }

    @Override
    public void handle(PostFeaturedEvent event) {
        notificationEventUseCase.sendPostFeaturedNotification(
                event.getUserId(),
                event.getSseMessage(),
                event.getPostId());
    }
}

