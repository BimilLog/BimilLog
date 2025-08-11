package jaeik.growfarm.domain.notification.application.port.out;

import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Users;

public interface SaveNotificationPort {
    void save(Users user, NotificationType type, String data, String url);
}
