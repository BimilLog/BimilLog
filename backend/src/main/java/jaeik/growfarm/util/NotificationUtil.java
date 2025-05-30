package jaeik.growfarm.util;

import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 알림 관련 유틸리티 클래스
 * 수정일 : 2025-05-03
 */
@Component
@RequiredArgsConstructor
public class NotificationUtil {

    /**
     * <h3>시간이 포함된 고유 ID 생성</h3>
     *
     * <p>
     * 사용자 ID와 현재 시간을 조합하여 고유한 ID를 생성한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param userId 사용자 ID
     * @return 시간이 포함된 고유 ID
     */
    public String makeTimeIncludeId(Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }

    /**
     * <h3>이벤트 DTO 생성</h3>
     *
     * <p>
     * 알림 타입, 데이터, URL을 포함한 이벤트 DTO를 생성한다.
     * </p>
     * 
     * @since 1.0.0
     * @author Jaeik
     * @param type 알림 타입
     * @param data 알림 데이터
     * @param url  알림 URL
     * @return 이벤트 DTO
     */
    public EventDTO createEventDTO(NotificationType type, String data, String url) {
        EventDTO eventDTO = new EventDTO();
        eventDTO.setType(type);
        eventDTO.setData(data);
        eventDTO.setUrl(url);
        return eventDTO;
    }
}
