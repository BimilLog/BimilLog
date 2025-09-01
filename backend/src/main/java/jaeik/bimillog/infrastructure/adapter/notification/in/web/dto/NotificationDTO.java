package jaeik.bimillog.infrastructure.adapter.notification.in.web.dto;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * <h2>알림 응답 DTO</h2>
 * <p>
 * 알림 조회 API 응답에 사용되는 데이터 전송 객체입니다.
 * 도메인의 NotificationInfo를 웹 계층의 형태로 변환합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String content;
    private String url;
    private NotificationType notificationType;
    private boolean isRead;
    private Instant createdAt;

}