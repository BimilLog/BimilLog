package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.dto.notification.EventDTO;
import jaeik.growfarm.entity.notification.NotificationType;
import jaeik.growfarm.entity.user.Setting;
import jaeik.growfarm.entity.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * <h2>알림 유틸리티 어댑터</h2>
 * <p>알림 이벤트 DTO 생성 등 유틸리티 기능을 제공하는 어댑터</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class NotificationUtilAdapter implements NotificationUtilPort {

    private final UserQueryUseCase userQueryUseCase;

    @Override
    public EventDTO createEventDTO(NotificationType type, String message, String url) {
        EventDTO eventDTO = new EventDTO();
        eventDTO.setType(type);
        eventDTO.setData(message);
        eventDTO.setUrl(url);
        return eventDTO;
    }

    @Override
    public String makeTimeIncludeId(Long userId, Long tokenId) {
        return userId + "_" + tokenId + "_" + System.currentTimeMillis();
    }

    @Override
    public boolean isEligibleForNotification(Long userId, NotificationType type) {
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return true;
        }

        Optional<Users> userOptional = userQueryUseCase.findById(userId);
        if (userOptional.isEmpty()) {
            return false;
        }

        Setting setting = userOptional.get().getSetting();
        return switch (type) {
            case FARM -> setting.isMessageNotification();
            case COMMENT -> setting.isCommentNotification();
            case POST_FEATURED -> setting.isPostFeaturedNotification();
            case COMMENT_FEATURED ->
                // COMMENT_FEATURED에 대한 설정은 현재 없으므로, 댓글 알림 설정을 따르도록 합니다.
                    setting.isCommentNotification();
            default -> false; // ADMIN, INITIATE는 위에서 처리했으므로 사실상 도달하지 않음
        };
    }
}