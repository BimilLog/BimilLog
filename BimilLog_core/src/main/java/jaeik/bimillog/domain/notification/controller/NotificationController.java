package jaeik.bimillog.domain.notification.controller;

import jaeik.bimillog.domain.global.entity.CustomUserDetails;
import jaeik.bimillog.domain.notification.dto.NotificationDTO;
import jaeik.bimillog.domain.notification.dto.UpdateNotificationDTO;
import jaeik.bimillog.domain.notification.entity.Notification;
import jaeik.bimillog.domain.notification.service.NotificationCommandService;
import jaeik.bimillog.domain.notification.service.NotificationQueryService;
import jaeik.bimillog.infrastructure.log.Log;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>알림 컨트롤러</h2>
 * <p>알림을 담당하는 REST API 컨트롤러입니다.</p>
 * <p>알림 목록 조회</p>
 * <p>알림 배치 삭제</p>
 *
 * @author Jaeik
 * @version 2.3.0
 */
@Log(level = Log.LogLevel.INFO,
        logExecutionTime = true,
        logParams = false,
        logResult = false)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {
    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandUseCase;

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>현재 로그인한 유저의 알림 리스트를 조회합니다.</p>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return ResponseEntity<List<NotificationDTO>> 알림 리스트 (최신순 정렬)
     * @author Jaeik
     * @since 2.3.0
     */
    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Notification> notifications = notificationQueryService.getNotificationList(userDetails);
        List<NotificationDTO> notificationDTOS = notifications.stream()
                .map(NotificationDTO::from)
                .toList();
        return ResponseEntity.ok(notificationDTOS);
    }

    /**
     * <h3>알림 배치 읽음/삭제 처리</h3>
     * <p>현재 로그인한 유저의 알림을 배치로 읽음 처리하거나 삭제합니다.</p>
     *
     * @param userDetails           현재 로그인한 유저 정보
     * @param updateNotificationDTO 알림 업데이트 정보 (읽음/삭제할 ID 목록)
     * @return ResponseEntity<Void> HTTP 응답
     * @author Jaeik
     * @since 2.3.0
     */
    @PostMapping("/update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        List<Long> readIds = updateNotificationDTO.getReadIds();
        List<Long> deletedIds = updateNotificationDTO.getDeletedIds();
        notificationCommandUseCase.batchUpdate(userDetails.getMemberId(), readIds, deletedIds);
        return ResponseEntity.ok().build();
    }
}
