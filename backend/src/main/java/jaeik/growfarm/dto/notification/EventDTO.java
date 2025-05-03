package jaeik.growfarm.dto.notification;

import jaeik.growfarm.entity.notification.NotificationType;
import lombok.Getter;
import lombok.Setter;

// SSE 알림 만들때 사용하는 DTO
@Getter
@Setter
public class EventDTO {

    NotificationType type;

    String data;

    String url;
}
