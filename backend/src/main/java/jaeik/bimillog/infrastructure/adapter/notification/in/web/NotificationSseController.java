package jaeik.bimillog.infrastructure.adapter.notification.in.web;

import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import jaeik.bimillog.infrastructure.auth.CustomUserDetails;
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
 * SSE(서버 전송 이벤트) 실시간 알림 구독 API를 처리하는 인바운드 어댑터입니다.
 * 클라이언트와의 SSE 연결을 관리하고 실시간 알림을 전송합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationSseController {

    private final NotificationSseUseCase notificationSSEUseCase;

    /**
     * <h3>SSE 구독</h3>
     *
     * @param userDetails 현재 로그인한 유저 정보
     * @return SSE 구독 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationSSEUseCase.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }
}