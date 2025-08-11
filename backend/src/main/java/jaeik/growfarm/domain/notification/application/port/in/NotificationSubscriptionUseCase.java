package jaeik.growfarm.domain.notification.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 유스케이스</h2>
 * <p>SSE 실시간 알림 구독 관련 비즈니스 로직을 정의하는 Primary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface NotificationSubscriptionUseCase {

    /**
     * <h3>SSE 구독</h3>
     * <p>사용자의 실시간 알림 구독을 처리합니다.</p>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return SSE Emitter
     */
    SseEmitter subscribe(Long userId, Long tokenId);
}