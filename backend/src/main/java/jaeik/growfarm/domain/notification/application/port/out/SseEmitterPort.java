package jaeik.growfarm.domain.notification.application.port.out;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <h2>SSE Emitter 포트</h2>
 * <p>SSE 연결 관리 관련 인프라 액세스를 정의하는 Secondary Port</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public interface SseEmitterPort {

    /**
     * <h3>SSE 구독</h3>
     *
     * @param userId  사용자 ID
     * @param tokenId 토큰 ID
     * @return SSE Emitter
     */
    SseEmitter subscribe(Long userId, Long tokenId);

    /**
     * <h3>사용자 SSE 연결 정리</h3>
     * <p>사용자와 관련된 모든 SSE Emitter 연결을 정리합니다.</p>
     *
     * @param userId 사용자 ID
     * @since 2.0.0
     * @author Jaeik
     */
    void deleteAllEmitterByUserId(Long userId);
}