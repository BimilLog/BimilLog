package jaeik.growfarm.dto.notification;

import jaeik.growfarm.entity.notification.Notification;
import jaeik.growfarm.entity.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 알림 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String data;
    private String url;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDTO fromEntity(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .data(notification.getData())
                .url(notification.getUrl())
                .type(notification.getNotificationType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}