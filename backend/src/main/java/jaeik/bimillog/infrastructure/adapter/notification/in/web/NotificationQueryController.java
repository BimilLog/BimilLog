package jaeik.bimillog.infrastructure.adapter.notification.in.web;

import jaeik.bimillog.domain.notification.application.port.in.NotificationQueryUseCase;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.infrastructure.adapter.notification.in.web.dto.NotificationDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>알림 조회 컨트롤러</h2>
 * <p>
 * 알림 조회 관련 REST API를 처리하는 인바운드 어댑터입니다.
 * 헥사고날 아키텍처에서 HTTP 웹 요청을 도메인으로 전달하는 역할을 담당합니다.
 * </p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationQueryController {

    private final NotificationQueryUseCase notificationQueryUseCase;

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>현재 로그인한 유저의 알림 리스트를 조회합니다.</p>
     * 
     * @param userDetails 현재 로그인한 유저 정보
     * @return ResponseEntity<List<NotificationDTO>> 알림 리스트
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Notification> notifications = notificationQueryUseCase.getNotificationList(userDetails);
        List<NotificationDTO> notificationDTOS = notifications.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(notificationDTOS);
    }

    /**
     * <h3>도메인 엔티티를 DTO로 변환</h3>
     * <p>Notification을 NotificationDTO로 변환합니다.</p>
     *
     * @param notification 도메인 알림 엔티티
     * @return NotificationDTO
     * @author Jaeik
     * @since 2.0.0
     */
    private NotificationDTO toDto(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .content(notification.getContent())
                .url(notification.getUrl())
                .notificationType(notification.getNotificationType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}