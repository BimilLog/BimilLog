package jaeik.growfarm.repository.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface EmitterRepository{

    SseEmitter save(String emitterId, SseEmitter sseEmitter);

    Map<String, SseEmitter> findAllEmitterByUserId(Long userId);

    void deleteById(String emitterId);

    void deleteAllEmitterByUserId(Long userId);

}
