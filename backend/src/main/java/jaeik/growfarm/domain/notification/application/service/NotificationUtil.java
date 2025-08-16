package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.EventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>알림 관련 유틸리티 클래스</h2>
 * <p>알림 시스템에서 공통으로 사용되는 유틸리티 메서드를 제공합니다.</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationUtil {

    /**
     * <h3>고유 Emitter ID 생성</h3>
     *
     * <p>유저 ID와 토큰 ID를 조합하여 고유한 Emitter ID를 생성한다.</p>
     * 
     * @since 2.0.0
     * @author Jaeik
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @return Emitter ID
     */
    public String makeTimeIncludeId(Long userId, Long tokenId) {
        return userId + "_" + tokenId + "_" + System.currentTimeMillis();
    }

    /**
     * <h3>이벤트 DTO 생성</h3>
     *
     * <p>
     * 알림 타입, 데이터, URL을 포함한 이벤트 DTO를 생성한다.
     * </p>
     * 
     * @since 2.0.0
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