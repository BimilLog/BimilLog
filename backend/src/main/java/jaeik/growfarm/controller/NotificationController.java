package jaeik.growfarm.controller;

import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserDTO().getUserId();
        return notificationService.subscribe(userId);
    }

    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserDTO().getUserId();
        List<NotificationDTO> notifications = notificationService.getNotificationList(userId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/batch-update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        notificationService.batchUpdate(updateNotificationDTO);
        return ResponseEntity.ok().build();
    }
}
