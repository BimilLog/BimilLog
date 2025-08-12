package jaeik.growfarm.global.listener;

import jaeik.growfarm.global.listener.handler.NotificationEventHandler;
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
     * <h3>알림 이벤트 처리</h3>
     * <p>
     *     발생한 모든 {@link ApplicationEvent}를 비동기적으로 수신합니다.
     *     자신이 처리할 수 있는 이벤트인지 각 핸들러에게 확인하고,
     *     처리할 수 있는 핸들러가 있으면 해당 핸들러의 handle 메서드를 호출합니다.
     * </p>
     * @param event 발생한 애플리케이션 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    @EventListener
    @Async("sseNotificationExecutor")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handleNotificationEvent(ApplicationEvent event) {
        for (NotificationEventHandler handler : notificationEventHandlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                return;
            }
        }
    }
}
