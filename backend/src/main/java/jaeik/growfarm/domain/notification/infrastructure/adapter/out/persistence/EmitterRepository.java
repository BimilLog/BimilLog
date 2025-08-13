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

    /**
     * <h3>Emitter 저장</h3>
     * <p>주어진 ID와 SSE Emitter 객체를 저장소에 저장합니다.</p>
     *
     * @param emitterId Emitter의 고유 ID
     * @param sseEmitter 저장할 SseEmitter 객체
     * @return 저장된 SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    /**
     * <h3>사용자 ID로 모든 Emitter 조회</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 Emitter를 조회합니다.</p>
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 ID에 해당하는 Emitter 맵 (Emitter ID -> SseEmitter)
     * @author Jaeik
     * @since 2.0.0
     */
    Map<String, SseEmitter> findAllEmitterByUserId(Long userId);

    /**
     * <h3>Emitter ID로 삭제</h3>
     * <p>주어진 Emitter ID에 해당하는 Emitter를 저장소에서 삭제합니다.</p>
     *
     * @param emitterId 삭제할 Emitter의 고유 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteById(String emitterId);

    /**
     * <h3>사용자 ID로 모든 Emitter 삭제</h3>
     * <p>주어진 사용자 ID에 해당하는 모든 Emitter를 저장소에서 삭제합니다.</p>
     *
     * @param userId 삭제할 Emitter를 소유한 사용자의 ID
     * @author Jaeik
     * @since 2.0.0
     */
    void deleteAllEmitterByUserId(Long userId);

}
