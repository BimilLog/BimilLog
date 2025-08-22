package jaeik.growfarm.infrastructure.adapter.notification.out.persistence.notification;

import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.notification.entity.NotificationType;
import jaeik.growfarm.domain.user.application.port.in.UserQueryUseCase;
import jaeik.growfarm.domain.user.entity.Setting;
import jaeik.growfarm.domain.user.entity.User;
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


    /**
     * <h3>시간 포함 Emitter ID 생성</h3>
     * <p>사용자 ID, 토큰 ID, 현재 시간을 조합하여 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param userId 사용자 ID
     * @param tokenId 토큰 ID
     * @return 생성된 Emitter ID 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public String makeTimeIncludeId(Long userId, Long tokenId) {
        return userId + "_" + tokenId + "_" + System.currentTimeMillis();
    }

    /**
     * <h3>알림 수신 자격 확인</h3>
     * <p>주어진 사용자 ID와 알림 유형에 따라 사용자가 알림을 수신할 자격이 있는지 확인합니다.</p>
     *
     * @param userId 확인할 사용자의 ID
     * @param type 확인할 알림 유형
     * @return 알림 수신이 가능하면 true, 그렇지 않으면 false
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public boolean isEligibleForNotification(Long userId, NotificationType type) {
        if (type == NotificationType.ADMIN || type == NotificationType.INITIATE) {
            return true;
        }

        Optional<User> userOptional = userQueryUseCase.findById(userId);
        if (userOptional.isEmpty()) {
            return false;
        }

        Setting setting = userOptional.get().getSetting();
        return switch (type) {
            case PAPER -> setting.isMessageNotification();
            case COMMENT -> setting.isCommentNotification();
            case POST_FEATURED -> setting.isPostFeaturedNotification();
            default -> false; // ADMIN, INITIATE는 위에서 처리했으므로 사실상 도달하지 않음
        };
    }
}