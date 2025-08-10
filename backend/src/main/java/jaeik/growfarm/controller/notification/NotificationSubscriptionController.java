package jaeik.growfarm.controller.notification;

import jaeik.growfarm.global.auth.CustomUserDetails;
import jaeik.growfarm.service.notification.NotificationFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 컨트롤러</h2>
 * <p>
 * SSE 실시간 알림 구독 API를 담당합니다.
 * SRP: SSE 구독 연결 관리만 담당
 * </p>
 *
 * @author Jaeik
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationSubscriptionController {

    private final NotificationFacadeService notificationFacadeService;

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
        return notificationFacadeService.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }
}