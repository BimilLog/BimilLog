package jaeik.growfarm.domain.admin.infrastructure.adapter.out.event;

import jaeik.growfarm.domain.admin.application.port.out.ManageEmitterPort;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * <h2>Emitter 영속성 어댑터</h2>
 * <p>
 * SSE Emitter(`SseEmitter`) 관련 데이터를 영속화하고 관리하는 Outgoing-Adapter
 * </p>
 * <p>
 * 관리자 도메인에서 사용자 Emitter 삭제 기능을 제공합니다.
 * </p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
public class EmitterPersistenceAdapter implements ManageEmitterPort {

    private final EmitterRepository emitterRepository;

    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        emitterRepository.deleteAllEmitterByUserId(userId);
    }
}
