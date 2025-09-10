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
 * <h2>SSE 실시간 알림 구독 컨트롤러</h2>
 * <p>
 * 실시간 알림 수신을 위한 SSE 구독 API를 제공합니다.
 * </p>
 * <p>
 * 프론트엔드의 알림 컴포넌트가 EventSource API를 통해 이 엔드포인트에 연결하여
 * 실시간 알림(댓글, 롤링페이퍼 메시지, 인기글 선정 등)을 즉시 받을 수 있도록 SSE 연결을 관리합니다.
 * </p>
 * <p>다른 도메인에서 발생하는 이벤트에 따른 실시간 브라우저 알림을 전송합니다.</p>
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
     * <h3>사용자 웹사이트 접속 시 실시간 알림 수신을 위한 SSE 구독 연결</h3>
     * <p>사용자가 웹 브라우저로 비밀로그에 접속한 상황에서 프론트엔드의 알림 헤더 컴포넌트나 
     * 알림 벨 아이콘이 렌더링될 때 EventSource API를 통해 이 엔드포인트로 연결하여 
     * 실시간 알림 스트림을 구독합니다.</p>
     * <p>댓글 작성, 롤링페이퍼 메시지 도착, 인기글 선정 등의 이벤트가 발생할 때마다 
     * NotificationGenerateListener가 이벤트를 처리하여 NotificationSseUseCase를 통해 
     * 이 SSE 연결로 실시간 알림 메시지를 전송하여 사용자가 즉시 확인할 수 있도록 합니다.</p>
     * <p>사용자별/토큰별로 구분된 SSE Emitter를 생성하여 멀티 디바이스 환경에서의 동시 접속을 지원합니다.</p>
     *
     * @param userDetails 현재 로그인한 사용자의 인증 정보 (사용자 ID, 토큰 ID 포함)
     * @return SseEmitter 실시간 알림 스트림 구독 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationSSEUseCase.subscribe(userDetails.getUserId(), userDetails.getTokenId());
    }
}