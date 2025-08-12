package jaeik.growfarm.domain.notification.infrastructure.adapter.in;

import jaeik.growfarm.domain.notification.application.port.in.NotificationQueryUseCase;
import jaeik.growfarm.dto.notification.NotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
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
 * <p>알림 조회 관련 API를 담당합니다.</p>
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
     */
    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationDTO> notificationDTOS = notificationQueryUseCase.getNotificationList(userDetails);
        return ResponseEntity.ok(notificationDTOS);
    }
}