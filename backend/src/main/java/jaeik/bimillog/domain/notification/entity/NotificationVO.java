package jaeik.bimillog.domain.notification.entity;

/**
 * <h2>알림 이벤트</h2>
 * <p>
 * 알림 전송에 필요한 비즈니스 데이터를 담는 도메인 엔티티입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public record NotificationVO(NotificationType type, String message, String url) {


    /**
     * <h3>알림 이벤트 생성</h3>
     * <p>주어진 파라미터로 새로운 알림 이벤트를 생성합니다.</p>
     *
     * @param type    알림 유형
     * @param message 알림 메시지
     * @param url     알림 URL
     * @return 생성된 알림 이벤트
     * @author Jaeik
     * @since 2.0.0
     */
    public static NotificationVO create(NotificationType type, String message, String url) {
        return new NotificationVO(type, message, url);
    }
}