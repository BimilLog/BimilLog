package jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * <h2>SSE Emitter Repository</h2>
 * <p>사용자별 SSE Emitter를 저장하고 관리하는 인터페이스</p>
 * <p>사용자의 실시간 알림 기능을 구현하기 위한 Emitter 저장소</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
public interface EmitterRepository {

    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    Map<String, SseEmitter> findAllEmitterByUserId(Long userId);

    void deleteById(String emitterId);

    void deleteAllEmitterByUserId(Long userId);

}
