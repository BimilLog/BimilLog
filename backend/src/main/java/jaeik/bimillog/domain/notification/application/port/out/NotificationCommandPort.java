package jaeik.bimillog.domain.notification.application.port.out;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import jaeik.bimillog.domain.user.entity.User;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;

/**
 * <h2>알림 저장 Port</h2>
 * <p>새로운 알림을 저장하는 아웃바운드 포트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationCommandPort {
    /**
     * <h3>알림 저장</h3>
     * <p>새로운 알림을 데이터베이스에 저장합니다.</p>
     *
     * @param user 알림을 받을 사용자 엔티티
     * @param type 알림 유형
     * @param content 알림 내용
     * @param url 알림 클릭 시 이동할 URL
     * @author Jaeik
     * @since 2.0.0
     */
    void save(User user, NotificationType type, String content, String url);

    /**
     * <h3>알림 일괄 업데이트</h3>
     *
     * @param userId           현재 로그인한 유저 ID
     * @param updateCommand 알림 업데이트 정보
     */
    void batchUpdate(Long userId, NotificationUpdateVO updateCommand);
}
