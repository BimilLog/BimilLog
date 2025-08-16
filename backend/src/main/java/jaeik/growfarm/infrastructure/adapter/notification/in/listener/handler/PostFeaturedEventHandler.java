package jaeik.growfarm.infrastructure.adapter.notification.in.listener.handler;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.domain.post.event.PostFeaturedEvent;
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

    /**
     * <h3>이벤트 지원 여부 확인</h3>
     * <p>주어진 이벤트가 {@link PostFeaturedEvent} 타입인지 확인합니다.</p>
     *
     * @param event 확인할 {@link ApplicationEvent}
     * @return 이벤트가 {@link PostFeaturedEvent} 타입이면 true, 아니면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean supports(ApplicationEvent event) {
        return event instanceof PostFeaturedEvent;
    }

    /**
     * <h3>이벤트 처리</h3>
     * <p>{@link PostFeaturedEvent}를 처리하여 인기 게시글 선정 알림을 전송합니다.</p>
     *
     * @param event 처리할 {@link PostFeaturedEvent}
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void handle(PostFeaturedEvent event) {
        notificationEventUseCase.sendPostFeaturedNotification(
                event.getUserId(),
                event.getSseMessage(),
                event.getPostId());
    }
}
