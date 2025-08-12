package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.domain.user.entity.User;

public interface SaveNotificationPort {
    void save(User user, NotificationType type, String content, String url);
}
