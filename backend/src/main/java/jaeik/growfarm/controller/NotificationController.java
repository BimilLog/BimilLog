package jaeik.growfarm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 
 * @author Jaeik
 * @version 1.0.0
 */
@Tag(name = "알림", description = "알림 관련 API")
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
    @Operation(summary = "SSE 알림 구독", description = "Server-Sent Events를 통해 실시간 알림을 구독합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "500", description = "SSE 알림 전송 중 오류 발생")
    })
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }

    /**
     * <h3>알림 리스트 조회</h3>
     * <p>
     * 현재 로그인한 유저의 알림 리스트를 조회합니다.
     * </p>
     * 
     * @param userDetails 현재 로그인한 유저 정보>
     * @return ResponseEntity<List<NotificationDTO>> 알림 리스트
     * @since 1.0.0
     * @author Jaeik
     */
    @Operation(summary = "알림 목록 조회", description = "현재 로그인한 사용자의 알림 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다.")
    })
    @GetMapping("/list")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationDTO> notificationDTOS = notificationService.getNotificationList(userDetails);
        return ResponseEntity.ok(notificationDTOS);
    }

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
    @Operation(summary = "알림 읽음/삭제 처리", description = "현재 로그인한 사용자의 알림을 읽음 처리하거나 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 업데이트 성공"),
            @ApiResponse(responseCode = "401", description = "유저 인증 정보가 없습니다."),
            @ApiResponse(responseCode = "403", description = "다른 사람의 알림은 처리할 수 없습니다.")
    })
    @PostMapping("/update")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "현재 로그인한 사용자 정보") @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 업데이트 정보") @RequestBody UpdateNotificationDTO updateNotificationDTO) {
        notificationService.batchUpdate(userDetails, updateNotificationDTO);
        return ResponseEntity.ok().build();
    }
}
