package jaeik.growfarm.controller;

import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.entity.notification.DeviceType;
import jaeik.growfarm.global.jwt.CustomUserDetails;
import jaeik.growfarm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/*
 * 알림 관련 API
 * SSE 구독
 * 알림 리스트 조회
 * 알림 읽음, 삭제 처리
 * FCM 토큰 등록
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    /*
     * SSE 구독 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * return: SseEmitter SSE 구독 객체
     * 수정일 : 2025-05-03
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserDTO().getUserId();
        return notificationService.subscribe(userId);
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
        Long userId = userDetails.getUserDTO().getUserId();
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

    /*
     * FCM 토큰 등록 API
     * param CustomUserDetails userDetails: 현재 로그인한 유저 정보
     * param String token: FCM 토큰
     * param DeviceType deviceType: 디바이스 타입
     * return: ResponseEntity<String> FCM 토큰 등록 완료 메시지
     * FCM 토큰은 핸드폰과 태블릿에서의 경우만 전달됨.
     * 수정일 : 2025-05-03
     */
    @PostMapping("/fcm/token")
    public ResponseEntity<String> registerFcmToken(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody String token,
            @RequestParam DeviceType deviceType) {
        Long userId = userDetails.getUserDTO().getUserId();
        notificationService.registerFcmToken(userId, token, deviceType);
        return ResponseEntity.status(200).build();
    }
}
