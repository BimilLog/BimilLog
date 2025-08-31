package jaeik.bimillog.infrastructure.adapter.notification.in.web.dto;

import jaeik.bimillog.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 알림 DTO
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