package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.domain.notification.domain.NotificationType;
import jaeik.growfarm.domain.user.domain.User;

public interface SaveNotificationPort {
    void save(User user, NotificationType type, String content, String url);
}
