package jaeik.growfarm.domain.notification.infrastructure.adapter.out;

import jaeik.growfarm.domain.notification.application.port.out.NotificationUtilPort;
import jaeik.growfarm.domain.notification.application.port.out.SseEmitterPort;
import jaeik.growfarm.domain.notification.domain.NotificationType;
import jaeik.growfarm.repository.notification.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * <h2>SSE Emitter 어댑터</h2>
 * <p>SSE 연결 관리를 위한 인프라 구현</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@Repository
@RequiredArgsConstructor
public class SseEmitterAdapter implements SseEmitterPort {

    private final EmitterRepository emitterRepository;
    private final NotificationUtilPort notificationUtilPort;

    @Override
    public SseEmitter subscribe(Long userId, Long tokenId) {
        String emitterId = notificationUtilPort.makeTimeIncludeId(userId, tokenId);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(Long.MAX_VALUE));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        sendNotification(emitter, emitterId, NotificationType.INITIATE,
                "EventStream Created. [emitterId=%s]".formatted(emitterId), "");

        return emitter;
    }

    private void sendNotification(SseEmitter emitter, String emitterId, NotificationType type, String data, String url) {
        String jsonData = String.format("{\"message\": \"%s\", \"url\": \"%s\"}",
                data, url);
        try {
            emitter.send(SseEmitter.event()
                    .name(type.toString())
                    .data(jsonData));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
        }
    }
}