package jaeik.growfarm.controller;

import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * <h2>알림 관련 컨트롤러</h2>
 * <p>
 * 알림 구독
 * </p>
 * <p>
 * 알림 리스트 조회
 * </p>
 * <p>
 * 알림 읽음/삭제 처리
 * </p>
 * @author Jaeik
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * <h3>SSE 구독</h3>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return SSE 구독 객체
     * @since 1.0.0
     * @author Jaeik
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }

    /*
     * 알림 리스트 조회 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: ResponseEntity<List<NotificationDTO>> 알림 리스트
     * 수정일 : 2025-05-03
     */
    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getClientDTO().getUserId();
        List<NotificationDTO> notifications = notificationService.getNotificationList(userId);
        return ResponseEntity.ok(notifications);
    }

    /*
     * 알림 읽음, 삭제 처리 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * param UpdateNotificationDTO updateNotificationDTO: 알림 읽음, 삭제 처리 DTO
     * return: ResponseEntity<Void> 알림 처리 완료 메시지
     * 수정일 : 2025-05-03
     */
    @PostMapping("/batch-update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        notificationService.batchUpdate(updateNotificationDTO);
        return ResponseEntity.ok().build();
    }
}
