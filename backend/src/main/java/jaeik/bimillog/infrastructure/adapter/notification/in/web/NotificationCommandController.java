package jaeik.bimillog.infrastructure.adapter.notification.in.web;

import jaeik.bimillog.domain.notification.application.port.in.NotificationCommandUseCase;
import jaeik.bimillog.domain.notification.entity.NotificationUpdateVO;
import jaeik.bimillog.infrastructure.adapter.notification.in.web.dto.UpdateNotificationDTO;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>알림 명령 컨트롤러</h2>
 * <p>
 * 알림 상태 변경 관련 REST API를 처리하는 인바운드 어댑터입니다.
 * 알림의 읽음 처리 및 삭제 요청을 도메인으로 전달합니다.
 * </p>
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
     * @author Jaeik
     * @since 2.0.0
     */
    @PostMapping("/update")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        NotificationUpdateVO updateCommand = toCommand(updateNotificationDTO);
        notificationCommandUseCase.batchUpdate(userDetails, updateCommand);
        return ResponseEntity.ok().build();
    }

    /**
     * <h3>DTO를 도메인 명령 객체로 변환</h3>
     * <p>UpdateNotificationDTO를 NotificationUpdateCommand로 변환합니다.</p>
     *
     * @param updateDto 알림 업데이트 DTO
     * @return NotificationUpdateVO
     * @author Jaeik
     * @since 2.0.0
     */
    private NotificationUpdateVO toCommand(UpdateNotificationDTO updateDto) {
        return NotificationUpdateVO.of(updateDto.getReadIds(), updateDto.getDeletedIds());
    }
}