package jaeik.growfarm.domain.admin.application.port.out;

import jaeik.growfarm.global.event.UserBannedEvent;

public interface SendNotificationPort {
    void publishEvent(UserBannedEvent event);
}
