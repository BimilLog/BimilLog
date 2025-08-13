package jaeik.growfarm.domain.notification.application.service;

import jaeik.growfarm.domain.notification.application.port.in.NotificationSubscriptionUseCase;
import jaeik.growfarm.domain.notification.application.port.out.SseEmitterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>알림 구독 서비스</h2>
 * <p>SSE 실시간 알림 구독 관련 비즈니스 로직을 처리하는 Use Case 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
public class NotificationSubscriptionService implements NotificationSubscriptionUseCase {

    private final SseEmitterPort sseEmitterPort;

    /**
     * <h3>알림 구독</h3>
     * <p>주어진 사용자 ID와 토큰 ID로 SSE 알림을 구독합니다.</p>
     *
     * @param userId 구독할 사용자의 ID
     * @param tokenId 구독 토큰 ID
     * @return SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter subscribe(Long userId, Long tokenId) {
        return sseEmitterPort.subscribe(userId, tokenId);
    }
}