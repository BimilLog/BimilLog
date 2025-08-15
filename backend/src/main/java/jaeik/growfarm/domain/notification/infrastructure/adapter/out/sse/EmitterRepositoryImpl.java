package jaeik.growfarm.domain.notification.infrastructure.adapter.out.sse;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h3>SseEmitter Repository 구현 클래스</h3>
 *
 * <p>SseEmitter 관련 메모리 작업을 정의합니다.</p>
 * <p>사용자 ID와 토큰 ID와 생성 시간을 조합하여 고유한 Emitter ID를 생성하고, 이를 통해 SseEmitter 객체를 저장 및 조회합니다.</p>
 *
 * @author Jaeik
 * @since 2.0.0
 */
@Repository
public class EmitterRepositoryImpl implements EmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * <h3>Emitter 저장</h3>
     *
     * <p>고유한 Emitter ID로 SseEmitter 객체를 저장합니다.</p>
     * <p>사용자 ID와 토큰 ID와 생성 시간을 조합하여 고유한 Emitter ID를 생성합니다.</p>
     *
     * @param emitterId  Emitter ID
     * @param sseEmitter SseEmitter 객체
     * @return 저장된 SseEmitter 객체
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    /**
     * <h3>사용자의 모든 Emitter 조회</h3>
     *
     * <p>사용자 ID에 해당하는 모든 Emitter를 조회합니다.</p>
     * <p>해당 사용자에 연결된 모든 기기로 알림을 보내는 용도입니다.</p>
     *
     * @param userId 유저 ID
     * @return 사용자 ID에 해당하는 모든 Emitter Map
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public Map<String, SseEmitter> findAllEmitterByUserId(Long userId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId.toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * <h3>사용자의 특정 Emitter 삭제</h3>
     *
     * <p>EmitterID로 특정한 SseEmitter 객체를 삭제합니다.</p>
     * <p>일시적 오류에서 사용합니다.</p>
     *
     * @param emitterId emitter ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    /**
     * <h3>사용자의 모든 Emitter 삭제</h3>
     *
     * <p>사용자 ID에 해당하는 모든 Emitter를 삭제합니다.</p>
     * <p>로그아웃, 회원탈퇴에서 사용합니다.</p>
     *
     * @param userId 유저 ID
     * @author Jaeik
     * @since 2.0.0
     */
    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        List<String> emitterIds = emitters.keySet().stream()
                .filter(key -> key.startsWith(userId.toString()))
                .toList();

        emitterIds.forEach(emitters::remove);
    }
}
