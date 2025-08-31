package jaeik.bimillog.domain.notification.entity;

import lombok.Builder;
import java.time.Instant;

/**
 * <h3>알림 정보 값 객체</h3>
 * <p>
 * 알림 조회 결과를 담는 도메인 순수 값 객체
 * NotificationDTO의 도메인 전용 대체
 * </p>
 *
 * @param id 알림 ID
 * @param content 내용
 * @param url 연결 URL
 * @param notificationType 알림 타입
 * @param isRead 읽음 여부
 * @param createdAt 생성일시
 * @author Jaeik
 * @since 2.0.0
 */
public record NotificationInfo(
        Long id,
        String content,
        String url,
        NotificationType notificationType,
        boolean isRead,
        Instant createdAt
) {

    @Builder
    public NotificationInfo {
    }

    /**
     * <h3>알림 정보 생성</h3>
     * <p>알림 엔티티로부터 정보를 생성합니다.</p>
     *
     * @param notification 알림 엔티티
     * @return NotificationInfo 값 객체
     */
    public static NotificationInfo from(Notification notification) {
        return NotificationInfo.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .url(notification.getUrl())
                .notificationType(notification.getNotificationType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}