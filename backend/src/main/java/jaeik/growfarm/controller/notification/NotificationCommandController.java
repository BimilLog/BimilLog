package jaeik.growfarm.controller.notification;

import jaeik.growfarm.dto.notification.UpdateNotificationDTO;
import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.notification.NotificationFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>알림 명령 컨트롤러</h2>
 * <p>
 * 알림의 상태를 변경하는 API를 담당합니다. (읽음/삭제 처리)
 * SRP: 알림 상태 변경(쓰기) 작업만 담당
 * </p>
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationCommandController {

    private final NotificationFacadeService notificationFacadeService;

    /**
     * <h3>알림 읽음/삭제 처리</h3>
     * <p>
     * 현재 로그인한 유저의 알림을 읽음 처리하거나 삭제합니다.
     * </p>
     * 
     * @param userDetails           현재 로그인한 유저 정보
     * @param updateNotificationDTO 알림 업데이트 정보
     * @return ResponseEntity<Void> HTTP 응답
     * @since 1.0.0
     * @author Jaeik
     */
    @PostMapping("/update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        notificationFacadeService.batchUpdate(userDetails, updateNotificationDTO);
        return ResponseEntity.ok().build();
    }
}