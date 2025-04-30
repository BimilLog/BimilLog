package jaeik.growfarm.util;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationUtil {

    public String makeTimeIncludeId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    public EventDTO createEventDTO(NotificationType type, String data, String url) {
        EventDTO eventDTO = new EventDTO();
        eventDTO.setType(type);
        eventDTO.setData(data);
        eventDTO.setUrl(url);
        return eventDTO;
    }
}
