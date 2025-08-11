package jaeik.growfarm.domain.admin.infrastructure.adapter.out.event;

import jaeik.growfarm.domain.admin.application.port.out.ManageEmitterPort;
import jaeik.growfarm.domain.notification.infrastructure.adapter.out.persistence.EmitterRepository;
import jaeik.growfarm.global.listener.handler.EmitterHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmitterPersistenceAdapter implements ManageEmitterPort {

    private final EmitterRepository emitterRepository;

    @Override
    public void deleteAllEmitterByUserId(Long userId) {
        emitterRepository.deleteAllEmitterByUserId(userId);
    }
}
