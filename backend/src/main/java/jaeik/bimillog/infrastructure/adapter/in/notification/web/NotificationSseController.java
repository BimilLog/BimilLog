package jaeik.bimillog.infrastructure.adapter.in.notification.web;

import jaeik.bimillog.domain.notification.application.port.in.SseUseCase;
import jaeik.bimillog.infrastructure.adapter.out.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE 실시간 알림 구독 컨트롤러</h2>
 * <p>SSE 실시간 알림 구독을 담당하는 REST API 컨트롤러입니다.</p>
 * <p>SSE 구독 연결</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationSseController {

    private final SseUseCase sseUseCase;

    /**
     * <h3>SSE 구독</h3>
     * <p>사용자가 로그인 시 실시간 알림 스트림을 구독합니다.</p>
     * <p>댓글 작성, 롤링페이퍼 메시지 도착, 인기글 선정 등의 이벤트가 발생할 때마다
     * NotificationGenerateListener로부터 이벤트를 처리하여 SSE메시지를 전송합니다</p>
     * <p>사용자별/토큰별로 구분된 SSE Emitter를 생성하여 멀티 디바이스 환경에서의 동시 접속을 지원합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자의 인증 정보 (사용자 ID, 토큰 ID 포함)
     * @return SseEmitter 실시간 알림 스트림 구독 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sseUseCase.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }
}