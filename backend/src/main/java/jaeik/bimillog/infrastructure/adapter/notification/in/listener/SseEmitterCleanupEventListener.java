package jaeik.bimillog.infrastructure.adapter.notification.in.listener;

import jaeik.bimillog.domain.auth.event.UserLoggedOutEvent;
import jaeik.bimillog.domain.notification.application.port.in.NotificationSseUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>SSE 연결 정리 이벤트 핸들러</h2>
 * <p>사용자 로그아웃 시 SSE 연결 정리를 담당하는 이벤트 핸들러</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterCleanupEventListener {

    private final NotificationSseUseCase notificationSseUseCase;

    /**
     * <h3>사용자 로그아웃 이벤트 처리 - SSE 연결 정리</h3>
     * <p>사용자가 로그아웃할 때 발생하는 이벤트를 구독하여 특정 기기의 SSE 연결 정리 작업을 수행</p>
     * <p>다중 기기 로그인 환경에서 로그아웃한 기기의 SSE 연결만 정리합니다.</p>
     *
     * @param event 사용자 로그아웃 이벤트
     * @since 2.0.0
     * @author Jaeik
     */
    @EventListener
    @Async
    public void handleUserLoggedOutEvent(UserLoggedOutEvent event) {
        try {
            log.debug("SSE 연결 정리 시작 - 사용자 ID: {}, 토큰 ID: {}", 
                     event.userId(), event.tokenId());
            
            // 특정 기기(토큰)의 SSE Emitter 연결만 정리
            notificationSseUseCase.deleteEmitterByUserIdAndTokenId(event.userId(), event.tokenId());
            
            log.info("SSE 연결 정리 완료 - 사용자 ID: {}, 토큰 ID: {}", 
                    event.userId(), event.tokenId());
            
        } catch (Exception e) {
            log.error("SSE 연결 정리 실패 - 사용자 ID: {}, 토큰 ID: {}, 오류: {}", 
                     event.userId(), event.tokenId(), e.getMessage(), e);
            // SSE 정리 실패는 로그아웃 자체를 실패시키지 않음 (비동기 처리)
        }
    }
}
