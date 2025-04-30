package jaeik.growfarm.dto.notification;

import jaeik.growfarm.entity.notification.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventDTO {

    NotificationType type;

    String data;

    String url;
}
