package jaeik.growfarm.domain.notification.entity;

/**
 * <h2>알림 이벤트</h2>
 * <p>
 * 알림 전송에 필요한 비즈니스 데이터를 담는 도메인 엔티티입니다.
 * 헥사고날 아키텍처에서 도메인 순수성을 보장하기 위해 외부 기술 의존성이 없는 순수한 도메인 모델입니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 * @since 2.0.0
 */
public class NotificationEvent {

    private final NotificationType type;
    private final String message;
    private final String url;

    /**
     * <h3>알림 이벤트 생성자</h3>
     * <p>알림 이벤트의 모든 필드를 초기화합니다.</p>
     *
     * @param type    알림 유형
     * @param message 알림 메시지
     * @param url     알림 URL
     * @author Jaeik
     * @since 2.0.0
     */
    private NotificationEvent(NotificationType type, String message, String url) {
        this.type = type;
        this.message = message;
        this.url = url;
    }

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
    public static NotificationEvent create(NotificationType type, String message, String url) {
        return new NotificationEvent(type, message, url);
    }

    /**
     * <h3>알림 유형 조회</h3>
     *
     * @return 알림 유형
     * @author Jaeik
     * @since 2.0.0
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * <h3>알림 메시지 조회</h3>
     *
     * @return 알림 메시지
     * @author Jaeik
     * @since 2.0.0
     */
    public String getMessage() {
        return message;
    }

    /**
     * <h3>알림 URL 조회</h3>
     *
     * @return 알림 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String getUrl() {
        return url;
    }
}