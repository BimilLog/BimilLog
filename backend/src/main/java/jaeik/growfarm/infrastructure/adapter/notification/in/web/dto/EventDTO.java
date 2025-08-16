package jaeik.growfarm.infrastructure.adapter.notification.in.web.dto;

import jaeik.growfarm.domain.notification.entity.NotificationType;
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
