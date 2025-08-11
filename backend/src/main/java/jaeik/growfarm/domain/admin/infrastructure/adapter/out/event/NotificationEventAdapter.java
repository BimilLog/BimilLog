package jaeik.growfarm.domain.admin.infrastructure.adapter.out.event;

import jaeik.growfarm.domain.admin.application.port.out.SendNotificationPort;
import jaeik.growfarm.global.event.UserBannedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventAdapter implements SendNotificationPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishEvent(UserBannedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
