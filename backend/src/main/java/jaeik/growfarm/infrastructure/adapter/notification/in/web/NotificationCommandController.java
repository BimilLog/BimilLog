package jaeik.growfarm.infrastructure.adapter.notification.in.web;

import jaeik.growfarm.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.growfarm.infrastructure.adapter.notification.in.web.dto.UpdateNotificationDTO;
import jaeik.growfarm.infrastructure.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>알림 명령 컨트롤러</h2>
 * <p>알림의 상태를 변경하는 API를 담당합니다. (읽음/삭제 처리)</p>
 * 
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationCommandController {

    private final NotificationCommandUseCase notificationCommandUseCase;

    /**
     * <h3>알림 읽음/삭제 처리</h3>
     * <p>현재 로그인한 유저의 알림을 읽음 처리하거나 삭제합니다.</p>
     * 
     * @param userDetails           현재 로그인한 유저 정보
     * @param updateNotificationDTO 알림 업데이트 정보
     * @return ResponseEntity<Void> HTTP 응답
     */
    @PostMapping("/update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        notificationCommandUseCase.batchUpdate(userDetails, updateNotificationDTO);
        return ResponseEntity.ok().build();
    }
}