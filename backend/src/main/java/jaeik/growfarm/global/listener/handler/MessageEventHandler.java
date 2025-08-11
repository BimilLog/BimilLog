package jaeik.growfarm.global.listener.handler;

import jaeik.growfarm.domain.notification.application.port.in.NotificationEventUseCase;
import jaeik.growfarm.global.event.MessageEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * <h2>롤링페이퍼 메시지 수신 이벤트 핸들러</h2>
 * <p>
 *     {@link MessageEvent}를 처리하여 롤링페이퍼 메시지 수신 알림을 전송합니다.
 * </p>
 *
 * @author jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class MessageEventHandler implements NotificationEventHandler<MessageEvent> {

    private final NotificationEventUseCase notificationEventUseCase;

    @Override
    public boolean supports(ApplicationEvent event) {
        return event instanceof MessageEvent;
    }

    @Override
    public void handle(MessageEvent event) {
        notificationEventUseCase.sendPaperPlantNotification(
                event.getPaperOwnerId(),
                event.getUserName());
    }
}

